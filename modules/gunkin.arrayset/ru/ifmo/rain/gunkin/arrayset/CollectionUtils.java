package ru.ifmo.rain.gunkin.arrayset;

import java.util.*;

public class CollectionUtils {
    public static <E> boolean isStrictlySorted(Collection<? extends E> collection, Comparator<? super E> comparator) {
        if (collection.isEmpty() || collection.size() == 1) {
            return true;
        }

        Iterator<? extends E> iter = collection.iterator();
        E prev = iter.next();
        while (iter.hasNext()) {
            E current = iter.next();

            if (compare(prev, current, comparator) >= 0) {
                return false;
            }

            prev = current;
        }
        return true;
    }

    public static <E> int compare(E e1, E e2, Comparator<? super E> comparator) {

        if (comparator == null) {
            //noinspection unchecked
            return ((Comparable<? super E>) e1).compareTo(e2);
        } else {
            return comparator.compare(e1, e2);
        }
    }

    public static <E> List<E> reverseView(List<E> list) {
        return new ReverseList<>(list);
    }

    private static class ReverseList<E> extends AbstractList<E> {
        private final List<E> forwardList;

        private ReverseList(List<E> forwardList) {
            this.forwardList = forwardList;
        }

        /**
         * {@inheritDoc}
         *
         * @param index
         * @throws IndexOutOfBoundsException {@inheritDoc}
         */
        @Override
        public E get(int index) {
            return forwardList.get(size() - 1 - index);
        }

        @Override
        public int size() {
            return forwardList.size();
        }
    }
}
