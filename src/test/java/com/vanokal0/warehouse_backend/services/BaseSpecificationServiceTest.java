package com.vanokal0.warehouse_backend.services;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class BaseSpecificationServiceTest {

    static class TestEntity {
    }

    static class TestSpecService extends BaseSpecificationService<TestEntity> {
        @Override
        protected Map<String, String> getFieldMapping() {
            return Map.of(
                    "name", "fullName",
                    "supplier", "supplier.companyName"
            );
        }
    }

    private final TestSpecService service = new TestSpecService();

    @Mock
    private Root<TestEntity> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path<Object> path;
    @Mock
    private Expression<String> lower;
    @Mock
    private Predicate predicate;
    @Mock
    private CriteriaBuilder.In<String> inClause;

    @BeforeEach
    void setUp() {
        doReturn(path).when(root).get(anyString());
        when(cb.lower(any())).thenReturn(lower);
        when(cb.in(lower)).thenReturn(inClause);
    }

    private Predicate apply(String operator, String value, Class<?> type) {
        doReturn(type).when(path).getJavaType();
        return service.getFilterSpec("field", operator, value).toPredicate(root, query, cb);
    }

    @Nested
    class BuildSort {

        @Test
        void returnsUnsorted_whenFieldsNull() {
            assertThat(service.buildSort(null, List.of("asc"))).isEqualTo(Sort.unsorted());
        }

        @Test
        void returnsUnsorted_whenOrdersNull() {
            assertThat(service.buildSort(List.of("name"), null)).isEqualTo(Sort.unsorted());
        }

        @Test
        void returnsUnsorted_whenSizesDiffer() {
            assertThat(service.buildSort(List.of("name", "x"), List.of("asc"))).isEqualTo(Sort.unsorted());
        }

        @Test
        void mapsFieldsAndDirectionsInOrder() {
            Sort sort = service.buildSort(List.of("name", "other"), List.of("DESC", "asc"));

            assertThat(sort.toList()).containsExactly(
                    Sort.Order.desc("fullName"),
                    Sort.Order.asc("other")
            );
        }

        @Test
        void treatsAnyNonDescValueAsAscending() {
            Sort sort = service.buildSort(List.of("supplier"), List.of("whatever"));

            assertThat(sort.getOrderFor("supplier.companyName").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        void returnsUnsorted_forEmptyLists() {
            assertThat(service.buildSort(List.of(), List.of()).isUnsorted()).isTrue();
        }
    }

    @Nested
    class BuildSpec {

        @Test
        void returnsNoOpSpec_whenAnyListIsNull() {
            assertThat(service.buildSpec(null, List.of(), List.of()).toPredicate(root, query, cb)).isNull();
            assertThat(service.buildSpec(List.of(), null, List.of()).toPredicate(root, query, cb)).isNull();
            assertThat(service.buildSpec(List.of(), List.of(), null).toPredicate(root, query, cb)).isNull();
            verifyNoInteractions(root);
        }

        @Test
        void returnsNoOpSpec_whenFieldsAndOperatorsSizesDiffer() {
            Specification<TestEntity> spec = service.buildSpec(List.of("name"), List.of(), List.of("x"));

            assertThat(spec.toPredicate(root, query, cb)).isNull();
            verifyNoInteractions(root);
        }

        @Test
        void appliesFieldMappingAndBuildsPredicate() {
            doReturn(String.class).when(path).getJavaType();
            when(cb.like(lower, "%jo%")).thenReturn(predicate);

            Predicate result = service.buildSpec(List.of("name"), List.of("contains"), List.of("Jo"))
                    .toPredicate(root, query, cb);

            assertThat(result).isSameAs(predicate);
            verify(root).get("fullName");
        }

        @Test
        void resolvesNestedFieldPaths() {
            Path<Object> supplierPath = mock(Path.class);
            doReturn(supplierPath).when(root).get("supplier");
            doReturn(path).when(supplierPath).get("companyName");
            doReturn(String.class).when(path).getJavaType();
            when(cb.equal(lower, "acme")).thenReturn(predicate);

            Predicate result = service.buildSpec(List.of("supplier"), List.of("equals"), List.of("ACME"))
                    .toPredicate(root, query, cb);

            assertThat(result).isSameAs(predicate);
        }

        @Test
        void skipsFiltersWithoutValues() {
            doReturn(String.class).when(path).getJavaType();

            service.buildSpec(List.of("a", "b"), List.of("equals", "equals"), List.of("x"))
                    .toPredicate(root, query, cb);

            verify(root).get("a");
            verify(root, never()).get("b");
        }

        @Test
        void combinesMultipleFilters() {
            doReturn(String.class).when(path).getJavaType();

            service.buildSpec(List.of("a", "b"), List.of("equals", "startsWith"), List.of("x", "y"))
                    .toPredicate(root, query, cb);

            verify(cb).equal(lower, "x");
            verify(cb).like(lower, "y%");
        }

        @Test
        void isAnyOf_joinsAllValuesWithoutConsumingThem() {
            doReturn(String.class).when(path).getJavaType();

            service.buildSpec(List.of("a", "b"), List.of("isAnyOf", "equals"), List.of("x", "y"))
                    .toPredicate(root, query, cb);

            verify(inClause).value("x");
            verify(inClause).value("y");
            // valueIndex is not advanced by isAnyOf, so the next filter reuses the first value
            verify(cb).equal(lower, "x");
        }
    }

    @Nested
    class StringOperators {

        @Test
        void equals_isCaseInsensitive() {
            when(cb.equal(lower, "abc")).thenReturn(predicate);
            assertThat(apply("equals", "AbC", String.class)).isSameAs(predicate);
        }

        @Test
        void doesNotEqual() {
            when(cb.notEqual(lower, "abc")).thenReturn(predicate);
            assertThat(apply("doesNotEqual", "ABC", String.class)).isSameAs(predicate);
        }

        @Test
        void contains() {
            when(cb.like(lower, "%abc%")).thenReturn(predicate);
            assertThat(apply("contains", "Abc", String.class)).isSameAs(predicate);
        }

        @Test
        void doesNotContain() {
            when(cb.notLike(lower, "%abc%")).thenReturn(predicate);
            assertThat(apply("doesNotContain", "abc", String.class)).isSameAs(predicate);
        }

        @Test
        void startsWith() {
            when(cb.like(lower, "abc%")).thenReturn(predicate);
            assertThat(apply("startsWith", "ABC", String.class)).isSameAs(predicate);
        }

        @Test
        void endsWith() {
            when(cb.like(lower, "%abc")).thenReturn(predicate);
            assertThat(apply("endsWith", "ABC", String.class)).isSameAs(predicate);
        }

        @Test
        void isEmpty_comparesRawPathToEmptyString() {
            when(cb.equal(path, "")).thenReturn(predicate);
            assertThat(apply("isEmpty", "", String.class)).isSameAs(predicate);
        }

        @Test
        void isNotEmpty_comparesRawPathToEmptyString() {
            when(cb.notEqual(path, "")).thenReturn(predicate);
            assertThat(apply("isNotEmpty", "", String.class)).isSameAs(predicate);
        }

        @Test
        void isAnyOf_splitsTrimsAndLowercases() {
            Predicate result = apply("isAnyOf", " Foo ,BAR", String.class);

            assertThat(result).isSameAs(inClause);
            verify(inClause).value("foo");
            verify(inClause).value("bar");
        }

        @Test
        void unknownOperator_returnsNull() {
            assertThat(apply("regex", "x", String.class)).isNull();
        }

        @Test
        void nullLiteral_producesIsNullRegardlessOfOperator() {
            when(cb.isNull(path)).thenReturn(predicate);
            assertThat(apply("contains", "NULL", String.class)).isSameAs(predicate);
        }
    }

    @Nested
    class ComparableOperators {

        @Test
        void isEmpty_producesIsNull() {
            when(cb.isNull(path)).thenReturn(predicate);
            assertThat(apply("is empty", "", Integer.class)).isSameAs(predicate);
        }

        @Test
        void isNotEmpty_producesIsNotNull() {
            when(cb.isNotNull(path)).thenReturn(predicate);
            assertThat(apply("is not empty", "", Integer.class)).isSameAs(predicate);
        }

        @ParameterizedTest
        @CsvSource({
                "=, EQ", "is, EQ",
                "!=, NE", "not, NE",
                ">, GT", "after, GT",
                ">=, GE", "onOrAfter, GE",
                "<, LT", "before, LT",
                "<=, LE", "onOrBefore, LE"
        })
        void comparisonOperators(String operator, String kind) {
            apply(operator, "5", Integer.class);

            Expression raw = path;
            Comparable five = 5;
            switch (kind) {
                case "EQ" -> verify(cb).equal(path, (Object) 5);
                case "NE" -> verify(cb).notEqual(path, (Object) 5);
                case "GT" -> verify(cb).greaterThan(raw, five);
                case "GE" -> verify(cb).greaterThanOrEqualTo(raw, five);
                case "LT" -> verify(cb).lessThan(raw, five);
                case "LE" -> verify(cb).lessThanOrEqualTo(raw, five);
                default -> throw new IllegalArgumentException(kind);
            }
        }

        @Test
        void between_parsesBothBounds() {
            Expression raw = path;
            when(cb.between(raw, (Comparable) LocalDate.of(2025, 1, 1), (Comparable) LocalDate.of(2025, 12, 31)))
                    .thenReturn(predicate);

            assertThat(apply("between", "2025-01-01;2025-12-31", LocalDate.class)).isSameAs(predicate);
        }

        @Test
        void between_withMalformedRange_returnsNull() {
            assertThat(apply("between", "2025-01-01", LocalDate.class)).isNull();
            Expression raw = path;
            verify(cb, never()).between(eq(raw), any(Comparable.class), any(Comparable.class));
        }

        @Test
        void unknownOperator_returnsNull() {
            assertThat(apply("~", "5", Integer.class)).isNull();
        }

        @Test
        void unparsableValue_returnsNull() {
            assertThat(apply("=", "not-a-number", Integer.class)).isNull();
            verify(cb, never()).equal(any(Expression.class), any(Object.class));
        }

        @Test
        void nonComparableType_returnsNull() {
            assertThat(apply("=", "x", List.class)).isNull();
        }
    }

    static Stream<Arguments> castCases() {
        return Stream.of(
                Arguments.of(Integer.class, "42", 42),
                Arguments.of(Long.class, "7", 7L),
                Arguments.of(BigDecimal.class, "12.50", new BigDecimal("12.50")),
                Arguments.of(LocalDate.class, "2025-03-04", LocalDate.of(2025, 3, 4)),
                Arguments.of(LocalDateTime.class, "2025-03-04T10:15:30", LocalDateTime.of(2025, 3, 4, 10, 15, 30)),
                Arguments.of(Instant.class, "2025-03-04", Instant.parse("2025-03-04T00:00:00Z")),
                Arguments.of(Instant.class, "2025-03-04T10:15:30Z", Instant.parse("2025-03-04T10:15:30Z")),
                Arguments.of(LocalDate.class,
                        "Tue Mar 04 2025 00:00:00 GMT+0200 (Eastern European Standard Time)",
                        LocalDate.of(2025, 3, 4)),
                Arguments.of(LocalDateTime.class,
                        "Tue Mar 04 2025 10:15:30 GMT+0200",
                        LocalDateTime.of(2025, 3, 4, 10, 15, 30))
        );
    }

    @ParameterizedTest
    @MethodSource("castCases")
    void castsValueToFieldType(Class<?> type, String raw, Object expected) {
        apply("=", raw, type);

        verify(cb).equal(path, expected);
    }

    @Test
    void jsDateString_isNotSupportedForInstantFields() {
        assertThat(apply("=", "Tue Mar 04 2025 10:15:30 GMT+0200", Instant.class)).isNull();
    }

    @Test
    void fieldEquals_usesResolvedPath() {
        Path<Object> supplierPath = mock(Path.class);
        doReturn(supplierPath).when(root).get("supplier");
        doReturn(path).when(supplierPath).get("companyName");
        when(cb.equal(path, (Object) "Acme")).thenReturn(predicate);

        Predicate result = service.fieldEquals("supplier.companyName", "Acme").toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }
}
