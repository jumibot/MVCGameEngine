package model.spatial.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import model.spatial.ports.SpatialGridStatisticsDTO;

/**
 * SpatialGrid (NEUTRAL + TOPOLOGÍA FIJA + PREALLOC)
 * 
 * El caller aporta un buffer temporal (scratchIdxs) reutilizable.
 *
 * Estructuras:
 * - buckets[idx] = ConcurrentHashMap<String, Boolean> (set de ids en esa celda)
 * - idToMembership[id] = celdas actuales del id (para remove/move O(1))
 *
 * Nota:
 * - queryCandidates puede devolver duplicados (si un body ocupa varias celdas).
 * Solución barata en colisiones: procesar solo si myId.compareTo(otherId) < 0.
 */
public final class SpatialGrid {

    private final double cellSize;
    private final int cellsX;
    private final int cellsY;
    private final int maxCellsPerBody;

    // ===== Datos =====
    // buckets[idx] = set(entityId)
    private final ConcurrentHashMap<String, Boolean>[] grid;

    // entityId -> celdas actuales (para remove/move O(1))
    private final ConcurrentHashMap<String, Cells> entitiesCells = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public SpatialGrid(double cellSize, int worldWidth, int worldHeight, int maxCellsPerBody) {
        if (cellSize <= 0)
            throw new IllegalArgumentException("cellSizePx must be > 0");
        if (worldWidth <= 0 || worldHeight <= 0)
            throw new IllegalArgumentException("world size must be > 0");
        if (maxCellsPerBody <= 0)
            throw new IllegalArgumentException("maxCellsPerBody must be > 0");

        this.cellSize = cellSize;
        this.maxCellsPerBody = maxCellsPerBody;

        // Ceil div
        this.cellsX = (int) ((worldWidth + cellSize - 1) / cellSize);
        this.cellsY = (int) ((worldHeight + cellSize - 1) / cellSize);

        final int total = this.cellsX * this.cellsY;

        // Prealloc buckets (arranque más caro, runtime estable)
        this.grid = (ConcurrentHashMap<String, Boolean>[]) new ConcurrentHashMap[total];
        for (int i = 0; i < total; i++) {
            this.grid[i] = new ConcurrentHashMap<>(8);
        }
    }

    public int getMaxCellsPerBody() {
        return maxCellsPerBody;
    }

    /**
     * Update cells useds by entityId according to posX,posY and size.
     * - Caller provides a reusable scratchIdxs buffer.
     * - Assumes no two threads are moving the same entityId at the same time.
     * - If the center is out of bounds, the entity is removed from the grid (the
     * domain will handle it).
     */
    public void upsert(
            String entityId, double minX, double maxX, double minY, double maxY, int[] scratchIdxs) {

        this.requireBuffer(scratchIdxs);

        if (entityId == null || entityId.isEmpty())
            return; // ======== Invalid ========>

        final Cells oldEntityCells = this.entitiesCells.computeIfAbsent(entityId, __ -> new Cells(maxCellsPerBody));

        // Contract: newCellIdxs[0..newCount) are valid, rest is garbage/previous
        final int[] newCellIdxs = scratchIdxs; // alias for semantic clarity: scratch used as "new cells"
        final int newCount = computeCellIdxsClamped(minX, maxX, minY, maxY, newCellIdxs);

        // olds not in news ---> body not in those cells anymore
        for (int i = 0; i < oldEntityCells.count; i++)
            if (!contains(newCellIdxs, newCount, oldEntityCells.idxs[i]))
                grid[oldEntityCells.idxs[i]].remove(entityId); // Remove entity from cell

        // news not in olds ---> body now in those cells
        for (int i = 0; i < newCount; i++)
            if (!contains(oldEntityCells.idxs, oldEntityCells.count, newCellIdxs[i]))
                grid[newCellIdxs[i]].put(entityId, Boolean.TRUE); // Add entity to cell

        // New are now old
        oldEntityCells.updateFrom(newCellIdxs, newCount);
    }

    /**
     * Use it when an entityId leaves the world or is removed.
     */
    public void remove(String entityId) {
        if (entityId == null || entityId.isEmpty())
            return;

        // Remove cells associated to entityId (from reverse mapping
        final Cells cells = this.entitiesCells.remove(entityId);
        if (cells == null)
            return;

        // Remove entityId from all associated cells (from grid)
        for (int i = 0; i < cells.count; i++) {
            grid[cells.idxs[i]].remove(entityId);
        }
    }

    /**
     * Returns collision candidates for the given entity.
     *
     * This method uses the entityId to retrieve the current cell membership
     * of the entity from the spatial grid (reverse mapping), and then iterates
     * all grid buckets corresponding to those cells to collect nearby entityIds.
     *
     * Important notes
     * ---------------
     * - Returned list may contain duplicates
     * - Iteration is weakly consistent: concurrent inserts/removals may be
     * observed, but the result is always safe and free of structural corruption.
     *
     * Performance characteristics
     * ---------------------------
     * - No spatial recomputation is performed (cell indices are reused).
     * - Time complexity is proportional to the number of occupied cells of the
     * entity and the number of entities in those cells.
     *
     * @param entityId the entity whose collision neighborhood is queried
     * @return the list of collision candidate entityIds (possibly empty) or null
     */
    public List<String> queryCollisionCandidates(String entityId) {
        if (entityId == null || entityId.isEmpty())
            return null;

        final Cells cells = this.entitiesCells.get(entityId);
        if (cells == null || cells.count <= 0)
            return null;

        List<String> out = new ArrayList<>(64);

        for (int i = 0; i < cells.count; i++) {
            final int cellIndex = cells.idxs[i];
            final ConcurrentHashMap<String, Boolean> bucket = this.grid[cellIndex];

            for (String id : bucket.keySet())
                if (!entityId.equals(id))
                    out.add(id);
        }

        return out;
    }

    /**
     * Samples runtime statistics of the spatial grid for monitoring and tuning
     * purposes.
     *
     * This method performs a full scan of all grid buckets and computes aggregated
     * metrics describing the current spatial distribution of entities. It is
     * intended for diagnostics, profiling, and validation of grid parameters (cell
     * size, load balance), not for use in hot paths.
     *
     * Collected metrics include:
     * - Number of non-empty buckets (buckets containing at least one entity).
     * - Number of empty buckets.
     * - Average number of entities per non-empty bucket.
     * - Maximum number of entities found in any single bucket.
     * - Total number of potential collision pairs (sum of nC2 per bucket).
     *
     * Performance notes:
     * - Time complexity is O(cellsX * cellsY)
     * - No allocations are performed during the scan.
     * - Call frequency should be low (e.g., debug mode, periodic monitoring).
     *
     * Concurrency notes:
     * - Iteration over buckets is weakly consistent; concurrent modifications may
     * be observed, but the computed statistics are always safe and structurally
     * consistent.
     *
     * @return a {@link SpatialGridStatisticsDTO} snapshot containing aggregated grid
     *         statistics
     */
    public SpatialGridStatisticsDTO getStatistics() {
        int nonEmptyBuckets = 0; // buckets with >=1 keys
        int emptyBuckets = 0; // buckets with 0 keys
        int maxBucketKeys = 0; // max keys in a bucket
        long totalKeys = 0; // total keys in all buckets
        long sumPairs = 0; // sum of nC2 for each bucket

        for (ConcurrentHashMap<String, Boolean> bucket : grid) {
            final int bucketSize = bucket.size();
            if (bucketSize <= 0) {
                emptyBuckets++;
                continue;
            }

            nonEmptyBuckets++;
            totalKeys += bucketSize;
            if (bucketSize > maxBucketKeys)
                maxBucketKeys = bucketSize;

            sumPairs += (long) bucketSize * (bucketSize - 1) / 2;
        }

        final double avgKeysPerBucketNotEmpty = (nonEmptyBuckets == 0) ? 0.0
                : ((double) totalKeys / (double) nonEmptyBuckets);

        return new SpatialGridStatisticsDTO(
                nonEmptyBuckets, emptyBuckets, avgKeysPerBucketNotEmpty, maxBucketKeys, sumPairs,
                cellSize, cellsX, cellsY, maxCellsPerBody);
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Computes the set of grid cell indices overlapped by an axis-aligned bounding
     * box (AABB),clamped to the fixed grid topology.
     *
     * The method converts the AABB defined by (minX, maxX, minY, maxY) into
     * discrete grid cell coordinates using integer division by the cell size, then
     * clamps those coordinates to the valid grid range [0 .. cellsX-1] and [0 ..
     * cellsY-1].
     *
     * All overlapped cell indices are written sequentially into {@code outIdxs},
     * and the number of valid entries is returned.
     *
     * Important notes:
     * - The valid range in {@code outIdxs} is [0 .. returnValue).
     * - The contents of {@code outIdxs} beyond that range are undefined.
     * - If number of cells exceeds {@code maxCellsPerBody}, the result is truncated
     * and a warning is emitted. In that case, {@code maxCellsPerBody}
     * should be increased.
     * - No allocations are performed; the caller must provide a reusable buffer.
     *
     * @param minX    the minimum X coordinate of the AABB (world space)
     * @param maxX    the maximum X coordinate of the AABB (world space)
     * @param minY    the minimum Y coordinate of the AABB (world space)
     * @param maxY    the maximum Y coordinate of the AABB (world space)
     * @param outIdxs a preallocated buffer where cell indices will be written
     * @return the number of grid cell indices written into {@code outIdxs}
     */
    private int computeCellIdxsClamped(
            double minX, double maxX, double minY, double maxY, int[] outIdxs) {

        int minCx = (int) (minX / cellSize);
        int maxCx = (int) (maxX / cellSize);
        int minCy = (int) (minY / cellSize);
        int maxCy = (int) (maxY / cellSize);

        // Clamp to the fixed grid topology
        minCx = this.clamp0Hi(minCx, cellsX - 1);
        maxCx = this.clamp0Hi(maxCx, cellsX - 1);
        minCy = this.clamp0Hi(minCy, cellsY - 1);
        maxCy = this.clamp0Hi(maxCy, cellsY - 1);

        int idx = 0;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cy = minCy; cy <= maxCy; cy++) {
                if (idx >= this.maxCellsPerBody) {
                    System.err.println(
                            "Warning: computeCellIdxsClamped() overflow. maxCellsPerBody="
                                    + this.maxCellsPerBody);
                    return idx; // if this happens, increase maxCellsPerBody (e.g. 9)
                }

                outIdxs[idx++] = cellIdx(cx, cy);
            }
        }
        return idx;
    }

    private int cellIdx(int cx, int cy) {
        return cy * this.cellsX + cx;
    }

    private void requireBuffer(int[] buf) {
        if (buf == null || buf.length < maxCellsPerBody)
            throw new IllegalArgumentException(
                    "idxsBuffer length must be >= maxCellsPerBody (" + maxCellsPerBody + ")");
    }

    private int clamp0Hi(int value, int highLimit) {
        return (value < 0) ? 0 : (value > highLimit) ? highLimit : value;
    }

    private static boolean contains(int[] arr, int count, int value) {
        for (int i = 0; i < count; i++)
            if (arr[i] == value)
                return true;

        return false;
    }
}
