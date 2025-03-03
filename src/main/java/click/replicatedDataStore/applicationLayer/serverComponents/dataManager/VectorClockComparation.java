package click.replicatedDataStore.applicationLayer.serverComponents.dataManager;

public enum VectorClockComparation {
    LESS_THAN(-1),
    EQUAL(0),
    GREATER_THAN(1),
    CONCURRENT(2);

    private final int compareResult;

    VectorClockComparation(int value){
        this.compareResult = value;
    }

    public int getCompareResult(){
        return compareResult;
    }
}
