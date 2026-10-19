package net.vulkanmod.render.chunk.util;

public final class CircularIntListCheck {
    public static void main(String[] args) {
        for (int size = 1; size <= 65; size++) {
            var list = new CircularIntList(size);
            var iterator = list.iterator();
            var complement = list.createRangeIterator();
            for (int start = 0; start < size; start++) {
                list.updateStartIdx(start);
                iterator.restart();
                for (int position = 0; position < size; position++) {
                    require(iterator.hasNext() && iterator.next() == (start + position) % size);
                    int index = iterator.getCurrentIndex();
                    require(list.getPrevious(index) == (position == 0 ? -1 : (start + position - 1) % size));
                    require(list.getNext(index) == (position == size - 1 ? -1 : (start + position + 1) % size));
                }
                require(!iterator.hasNext() && list.iterator() == iterator);
                var range = list.getRangeIterator(0, size / 2 - 1);
                complement.update(size / 2, size - 1);
                int count = 0;
                while (range.hasNext()) require(range.next() == (start + count++) % size);
                while (complement.hasNext()) require(complement.next() == (start + count++) % size);
                require(count == size);
            }
        }
        System.out.println("Circular grid PASS: all rotations/ranges, boundary neighbors, independent reused iterators");
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalStateException("Circular grid traversal mismatch");
    }
}
