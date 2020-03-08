package ru.ifmo.rain.gunkin.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {
    private final Function<Student, String> FULL_NAME_GETTER = s -> s.getFirstName() + " " + s.getLastName();
    private final Comparator<Student> NAME_COMPARATOR = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    private <T, U extends Collection<T>> U getMappedStudents(List<Student> students, Function<Student, T> mapper, Collector<T, ?, U> collector) {
        return students.stream()
                .map(mapper)
                .collect(collector);
    }


    private ToLongFunction<List<Student>> distinctValuesCounter(Function<Student, String> mapper) {
        return l -> l.stream().map(mapper).distinct().count();
    }

    /**
     * Returns student {@link Student#getFirstName() first names}.
     */
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getMappedStudents(students, Student::getFirstName, Collectors.toList());
    }

    /**
     * Returns student {@link Student#getLastName() last names}.
     */
    @Override
    public List<String> getLastNames(List<Student> students) {
        return getMappedStudents(students, Student::getLastName, Collectors.toList());
    }

    /**
     * Returns student {@link Student#getGroup() groups}.
     */
    @Override
    public List<String> getGroups(List<Student> students) {
        return getMappedStudents(students, Student::getGroup, Collectors.toList());
    }

    /**
     * Returns full student name.
     */
    @Override
    public List<String> getFullNames(List<Student> students) {
        return getMappedStudents(students, FULL_NAME_GETTER, Collectors.toList());
    }

    /**
     * Returns distinct student {@link Student#getFirstName() first names} in alphabetical order.
     */
    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getMappedStudents(students, Student::getFirstName, Collectors.toCollection(TreeSet::new));
    }


    private Stream<Student> sortedStudentsBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator);
    }


    /**
     * Returns name of the student with minimal {@link Student#getId() id}.
     */
    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return sortedStudentsBy(students, Student::compareTo).findFirst().map(Student::getFirstName).orElse("");
    }

    private List<Student> sortedStudentListBy(Collection<Student> students, Comparator<Student> comparator) {
        return sortedStudentsBy(students, comparator).collect(Collectors.toList());
    }

    /**
     * Returns list of students sorted by {@link Student#getId() id}.
     */
    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedStudentListBy(students, Student::compareTo);
    }

    /**
     * Returns list of students sorted by name
     * (students are ordered by {@link Student#getLastName() lastName},
     * students with equal last names are ordered by {@link Student#getFirstName() firstName},
     * students having equal both last and first names are ordered by {@link Student#getId() id}.
     */
    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedStudentListBy(students, NAME_COMPARATOR);
    }


    private <T> List<Student> findStudentsBy(Collection<Student> students, T value, Function<Student, T> mapper) {
        return students.stream()
                .filter(s -> Objects.equals(value, mapper.apply(s)))
                .sorted(NAME_COMPARATOR)
                .collect(Collectors.toList());
    }

    /**
     * Returns list of students having specified first name. Students are ordered by name.
     */
    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, name, Student::getFirstName);
    }

    /**
     * Returns list of students having specified last name. Students are ordered by name.
     */
    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, name, Student::getLastName);
    }

    /**
     * Returns list of students having specified groups. Students are ordered by name.
     */
    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsBy(students, group, Student::getGroup);
    }


    /**
     * Returns map of group's student last names mapped to minimal first name.
     */
    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.groupingBy(
                        Student::getLastName,
                        Collectors.collectingAndThen(Collectors.toList(), l -> l.get(0).getFirstName())));
    }


    private <T> Stream<Map.Entry<T, List<Student>>> groupingStudentsBy(Collection<Student> students, Function<Student, T> classifier) {
        return students.stream()
                .collect(Collectors.groupingBy(classifier))
                .entrySet().stream();
    }

    /**
     * Returns student groups, where groups are ordered by name, and students within a group are ordered by passed comparator.
     */
    public List<Group> getSortedGroups(Collection<Student> students, Comparator<Student> comparator) {
        return groupingStudentsBy(students, Student::getGroup)
                .map(e -> new Group(e.getKey(), e.getValue().stream().sorted(comparator).collect(Collectors.toList())))
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());

    }

    /**
     * Returns student groups, where both groups and students within a group are ordered by name.
     */
    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getSortedGroups(students, NAME_COMPARATOR);
    }

    /**
     * Returns student groups, where groups are ordered by name, and students within a group are ordered by id.
     */
    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getSortedGroups(students, Student::compareTo);
    }

    private String getMaxFromGroupedStudents(Collection<Student> students,
                                             Function<Student, String> groupClassifier,
                                             ToLongFunction<List<Student>> listMapper,
                                             Comparator<String> keyComparator) {
        return groupingStudentsBy(students, groupClassifier)
                .max(Comparator
                        .comparingLong((Map.Entry<String, List<Student>> e)
                                -> listMapper.applyAsLong(e.getValue()))
                        .thenComparing(Map.Entry::getKey, keyComparator))
                .map(Map.Entry::getKey).orElse("");
    }

    public String getLargestGroupWith(Collection<Student> students, ToLongFunction<List<Student>> function) {
        return getMaxFromGroupedStudents(students, Student::getGroup, function, Collections.reverseOrder(String::compareTo));
    }

    /**
     * Returns name of the group containing maximum number of students.
     * If there are more than one largest group, the one with smallest name is returned.
     */
    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupWith(students, List::size);
    }

    /**
     * Returns name of the group containing maximum number of students with distinct first names.
     * If there are more than one largest group, the one with smallest name is returned.
     */
    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupWith(students, distinctValuesCounter(Student::getFirstName));
    }


    /**
     * Returns the name of the student such that most number of groups has student with that name.
     * If there are more than one such name, the largest one is returned.
     */
    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getMaxFromGroupedStudents(students, FULL_NAME_GETTER, distinctValuesCounter(Student::getGroup), String::compareTo);
    }

    private List<Student> getListByIndices(List<Student> students, final int[] indices) {
        return Arrays.stream(indices).mapToObj(students::get).collect(Collectors.toList());
    }

    private List<Student> getListByIndices(Collection<Student> students, final int[] indices) {
        return  getListByIndices(new ArrayList<>(students), indices);
    }

    /**
     * Returns student {@link Student#getFirstName() first names} by indices.
     */
    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getFirstNames(getListByIndices(students, indices));
    }

    /**
     * Returns student {@link Student#getLastName() last names} by indices.
     */
    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getLastNames(getListByIndices(students, indices));
    }

    /**
     * Returns student {@link Student#getGroup() groups} by indices.
     */
    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getGroups(getListByIndices(students, indices));
    }

    /**
     * Returns full student name by indices.
     */
    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getFullNames(getListByIndices(students, indices));
    }
}
