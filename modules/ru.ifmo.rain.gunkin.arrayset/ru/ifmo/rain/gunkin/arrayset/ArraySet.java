package ru.ifmo.rain.gunkin.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E>
        implements NavigableSet<E> {

    private final List<E> data;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this(List.of(), null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(List.of(), comparator);
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        this.comparator = comparator;

        if (!CollectionUtils.isStrictlySorted(collection, comparator)) {
            TreeSet<E> set = new TreeSet<>(comparator);
            set.addAll(collection);
            this.data = List.copyOf(set);
        } else {
            if (collection.stream().anyMatch(Objects::isNull)) {
                throw new NullPointerException("Collection must not contain null");
            }
            this.data = List.copyOf(collection);
        }

    }

    private ArraySet(List<E> subList, Comparator<? super E> comparator) {
        this.data = subList;
        this.comparator = comparator;
    }

    private NavigableSet<E> emptySet() {
        return new ArraySet<>(comparator);
    }

    @Override
    public boolean contains(Object o) {
        //noinspection unchecked
        return Collections.binarySearch(data, (E) o, comparator) >= 0;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E lower(E e) {
        int index = lowerBound(e, false);
        return index == -1 ? null : data.get(index);

    }

    @Override
    public E floor(E e) {
        int index = lowerBound(e, true);
        return index == -1 ? null : data.get(index);
    }

    @Override
    public E ceiling(E e) {
        int index = upperBound(e, true);
        return index == size() ? null : data.get(index);
    }

    @Override
    public E higher(E e) {
        int index = upperBound(e, false);
        return index == size() ? null : data.get(index);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(size() - 1);
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(CollectionUtils.reverseView(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return subSet(false, fromElement, fromInclusive,
                false, toElement, toInclusive);
    }


    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return subSet(true, null, true,
                false, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return subSet(false, fromElement, inclusive,
                true, null, true);
    }


    @Override
    public NavigableSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    private NavigableSet<E> subSet(boolean fromStart, E fromElement, boolean fromInclusive,
                                   boolean toEnd, E toElement, boolean toInclusive) {
        if (!fromStart && !toEnd) {
            if (CollectionUtils.compare(fromElement, toElement, comparator) > 0) {
                throw new IllegalArgumentException();
            }
        }

        int l = fromStart ? 0 : upperBound(fromElement, fromInclusive);
        int r = toEnd ? size() - 1 : lowerBound(toElement, toInclusive);

        if (l > r) {
            return emptySet();
        }

        return new ArraySet<>(data.subList(l, r + 1), comparator);

    }

    private int binarySearch(E element) {
        return Collections.binarySearch(data, element, comparator);
    }

    private int lowerBound(E element, boolean inclusive) {
        int index = binarySearch(element);
        return index < 0
                ? -(index + 1) - 1
                : (inclusive ? index : index - 1);
    }


    private int upperBound(E element, boolean inclusive) {
        int index = binarySearch(element);
        return index < 0
                ? -(index + 1)
                : (inclusive ? index : index + 1);
    }


}

