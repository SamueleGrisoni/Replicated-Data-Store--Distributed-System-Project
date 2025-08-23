package click.replicatedDataStore.applicationLayer.serverComponents;

import click.replicatedDataStore.dataStructures.ClockedData;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

public class BackupList implements Serializable {
    private final List<ClockedData> list;
    private transient Comparator<ClockedData> comparator;

    public BackupList() {
        this.list = new java.util.ArrayList<>();
        this.comparator = Comparator.comparing(ClockedData::vectorClock);
    }

    public void add(ClockedData clockedData) {
        list.add(clockedData);
        list.sort(comparator);
    }

    public void add(List<ClockedData> clockedDataList) {
        list.addAll(clockedDataList);
        list.sort(comparator);
    }

    public List<ClockedData> getClockedDataSinceIndex(int index) {
        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }
        return list.subList(index, list.size());
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BackupList{ \n");
        int i = 0;
        for (ClockedData clockedData : list) {
            sb.append("[").append(i++).append(": ").append(clockedData.vectorClock()).append(", ").append(clockedData.key()).append(", ").append(clockedData.value()).append("], ").append("\n");
        }
        if (!list.isEmpty()) {
            sb.delete(sb.length() - 3, sb.length()); // Remove the last comma and newline
        }
        sb.append("}");
        return sb.toString();
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.comparator = Comparator.comparing(ClockedData::vectorClock);
    }
}