package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.WarehouseDTO;
import com.vanokal0.warehouse_backend.dto.lightweight.WarehouseCreateUpdateDTO;
import com.vanokal0.warehouse_backend.mappers.WarehouseMapper;
import com.vanokal0.warehouse_backend.model.Warehouse;
import com.vanokal0.warehouse_backend.repositories.WarehouseRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.vanokal0.warehouse_backend.TestFixtures.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    private static final int MANAGER_ID = 110;

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private WarehouseMapper warehouseMapper;
    @Mock
    private ActionLogService actionLogService;

    @InjectMocks
    private WarehouseService service;

    @Captor
    private ArgumentCaptor<Map<String, Object>> prevCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Object>> newCaptor;

    private static WarehouseCreateUpdateDTO dto(String number, String address) {
        WarehouseCreateUpdateDTO dto = new WarehouseCreateUpdateDTO();
        dto.setNumber(number);
        dto.setAddress(address);
        return dto;
    }

    @Nested
    class Queries {

        @Test
        void getAllWarehouses_returnsOnlyActiveOnes() {
            Warehouse active = warehouse(1, "MA1", "Addr 1", true);
            Warehouse inactive = warehouse(2, "MA2", "Addr 2", false);
            WarehouseDTO activeDto = new WarehouseDTO();
            when(warehouseRepository.findAll()).thenReturn(List.of(active, inactive));
            when(warehouseMapper.toDTO(active)).thenReturn(activeDto);

            assertThat(service.getAllWarehouses()).containsExactly(activeDto);
            verify(warehouseMapper, never()).toDTO(inactive);
        }

        @Test
        void getWarehouseByNumber_returnsMappedActiveWarehouse() {
            Warehouse w = warehouse(1, "MA1", "Addr", true);
            WarehouseDTO expected = new WarehouseDTO();
            when(warehouseRepository.findByNumber("MA1")).thenReturn(Optional.of(w));
            when(warehouseMapper.toDTO(w)).thenReturn(expected);

            assertThat(service.getWarehouseByNumber("MA1")).isSameAs(expected);
        }

        @Test
        void getWarehouseByNumber_throws_whenInactive() {
            when(warehouseRepository.findByNumber("MA1")).thenReturn(Optional.of(warehouse(1, "MA1", "Addr", false)));

            assertThatThrownBy(() -> service.getWarehouseByNumber("MA1"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("MA1");
            verifyNoInteractions(warehouseMapper);
        }

        @Test
        void getWarehouseByNumber_throws_whenMissing() {
            when(warehouseRepository.findByNumber("X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getWarehouseByNumber("X"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("X");
        }
    }

    @Nested
    class Create {

        @Test
        void throws_whenNumberAlreadyExists() {
            when(warehouseRepository.existsByNumber("MA1")).thenReturn(true);

            assertThatThrownBy(() -> service.createWarehouse(dto("MA1", "Addr")))
                    .isInstanceOf(NoSuchElementException.class);
            verify(warehouseRepository, never()).save(any());
            verifyNoInteractions(actionLogService);
        }

        @Test
        void savesActiveWarehouse_logsCreate_andReturnsDto() {
            WarehouseDTO expected = new WarehouseDTO();
            when(warehouseRepository.existsByNumber("MA9")).thenReturn(false);
            when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> {
                Warehouse w = inv.getArgument(0);
                w.setId(99);
                return w;
            });
            when(warehouseMapper.toDTO(any(Warehouse.class))).thenReturn(expected);

            WarehouseDTO result = service.createWarehouse(dto("MA9", "New street 1"));

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
            verify(warehouseRepository).save(captor.capture());
            assertThat(captor.getValue().getNumber()).isEqualTo("MA9");
            assertThat(captor.getValue().getAddress()).isEqualTo("New street 1");
            assertThat(captor.getValue().getIsActive()).isTrue();
            verify(actionLogService).logAction(MANAGER_ID, "CREATE", "Warehouse", 99, null, null);
        }
    }

    @Nested
    class Update {

        @Test
        void throws_whenWarehouseNotFound() {
            when(warehouseRepository.findById(5)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateWarehouse(5, dto("MA1", "Addr")))
                    .isInstanceOf(NoSuchElementException.class);
            verify(warehouseRepository, never()).save(any());
        }

        @Test
        void throws_whenNewNumberBelongsToAnotherWarehouse() {
            Warehouse w = warehouse(5, "MA1", "Addr", true);
            when(warehouseRepository.findById(5)).thenReturn(Optional.of(w));
            when(warehouseRepository.existsByNumber("MA2")).thenReturn(true);

            assertThatThrownBy(() -> service.updateWarehouse(5, dto("MA2", "Addr")))
                    .isInstanceOf(NoSuchElementException.class);
            assertThat(w.getNumber()).isEqualTo("MA1");
            verify(warehouseRepository, never()).save(any());
            verifyNoInteractions(actionLogService);
        }

        @Test
        void keepingSameNumber_skipsUniquenessCheck() {
            Warehouse w = warehouse(5, "MA1", "Old addr", true);
            when(warehouseRepository.findById(5)).thenReturn(Optional.of(w));
            when(warehouseRepository.save(w)).thenReturn(w);

            service.updateWarehouse(5, dto("MA1", "New addr"));

            verify(warehouseRepository, never()).existsByNumber(anyString());
            assertThat(w.getAddress()).isEqualTo("New addr");
        }

        @Test
        void updatesFields_logsPrevAndNewState_andReturnsDto() {
            Warehouse w = warehouse(5, "MA1", "Old addr", true);
            WarehouseDTO expected = new WarehouseDTO();
            when(warehouseRepository.findById(5)).thenReturn(Optional.of(w));
            when(warehouseRepository.existsByNumber("MA7")).thenReturn(false);
            when(warehouseRepository.save(w)).thenReturn(w);
            when(warehouseMapper.toDTO(w)).thenReturn(expected);

            WarehouseDTO result = service.updateWarehouse(5, dto("MA7", "New addr"));

            assertThat(result).isSameAs(expected);
            assertThat(w.getNumber()).isEqualTo("MA7");
            assertThat(w.getAddress()).isEqualTo("New addr");
            verify(actionLogService).logAction(eq(MANAGER_ID), eq("UPDATE"), eq("Warehouse"), eq(5),
                    prevCaptor.capture(), newCaptor.capture());
            assertThat(prevCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of("number", "MA1", "address", "Old addr"));
            assertThat(newCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of("number", "MA7", "address", "New addr"));
        }
    }

    @Nested
    class Archive {

        @Test
        void throws_whenWarehouseNotFound() {
            when(warehouseRepository.findById(5)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.archiveWarehouse(5)).isInstanceOf(NoSuchElementException.class);
            verifyNoInteractions(actionLogService);
        }

        @Test
        void deactivatesWarehouse_andLogsDelete() {
            Warehouse w = warehouse(5, "MA1", "Addr", true);
            when(warehouseRepository.findById(5)).thenReturn(Optional.of(w));

            service.archiveWarehouse(5);

            assertThat(w.getIsActive()).isFalse();
            verify(warehouseRepository).save(w);
            verify(actionLogService).logAction(eq(MANAGER_ID), eq("DELETE"), eq("Warehouse"), eq(5),
                    prevCaptor.capture(), isNull());
            assertThat(prevCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of("number", "MA1", "address", "Addr"));
        }
    }
}
