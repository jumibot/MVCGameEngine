package view.renderables.ports;

public class SpatialGridStatisticsRenderDTO {
    public final int nonEmptyCells;
    public final int emptyCells;
    public final double avgBucketSizeNonEmpty;
    public final int maxBucketSize;
    public final long estimatedPairChecks;

    public final double cellSize;
    public final int cellsX;
    public final int cellsY;
    public final int maxCellsPerBody;

    public SpatialGridStatisticsRenderDTO(
            int nonEmptyCells,
            int emptyCells,
            double avgBucketSizeNonEmpty,
            int maxBucketSize,
            long estimatedPairChecks,

            double cellSize,
            int cellsX,
            int cellsY,
            int maxCellsPerBody) {

        this.nonEmptyCells = nonEmptyCells;
        this.emptyCells = emptyCells;
        this.avgBucketSizeNonEmpty = avgBucketSizeNonEmpty;
        this.maxBucketSize = maxBucketSize;
        this.estimatedPairChecks = estimatedPairChecks;

        this.cellSize = cellSize;
        this.cellsX = cellsX;
        this.cellsY = cellsY;
        this.maxCellsPerBody = maxCellsPerBody;
    }

    public Object[] toObjectArray() {
        return new Object[] {
                this.cellSize,
                this.nonEmptyCells + this.emptyCells,
                (double)this.emptyCells/(double)(this.nonEmptyCells + this.emptyCells),
                String.format("%.1f", this.avgBucketSizeNonEmpty),
                this.maxBucketSize,
                this.estimatedPairChecks,
        };
    }
}
