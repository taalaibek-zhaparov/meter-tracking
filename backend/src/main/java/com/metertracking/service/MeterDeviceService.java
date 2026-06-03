package com.metertracking.service;

import com.metertracking.dto.MeterDeviceDTO;
import com.metertracking.entity.MeterDevice;
import com.metertracking.repository.MeterDeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MeterDeviceService {

    @Autowired
    private MeterDeviceRepository meterDeviceRepository;

    public List<MeterDeviceDTO> searchByMeterNumber(String query) {
        return meterDeviceRepository
                .searchByMeterNumber(query, PageRequest.of(0, 10))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<MeterDeviceDTO> getByMeterNumber(String meterNumber) {
        return meterDeviceRepository.findByMeterNumber(meterNumber).map(this::toDTO);
    }

    public List<MeterDeviceDTO> getAll() {
        // Используем JOIN FETCH чтобы подтянуть модель из справочника
        return meterDeviceRepository.findAllWithModel().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public MeterDeviceDTO create(MeterDeviceDTO dto) {
        MeterDevice device = new MeterDevice();
        device.setMeterNumber(dto.getMeterNumber());
        device.setMeterType(dto.getMeterType());
        device.setSimCardNumber(dto.getSimCardNumber());
        device.setIccidNumber(dto.getIccidNumber());
        device.setSealNumber(dto.getSealNumber());
        device.setPhases(dto.getPhases());
        device.setAmperage(dto.getAmperage());
        device.setZnch(dto.getZnch());
        device.setVoltage(dto.getVoltage());
        device.setAvailable(true);
        return toDTO(meterDeviceRepository.save(device));
    }

    public MeterDeviceDTO update(Long id, MeterDeviceDTO dto) {
        MeterDevice device = meterDeviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Счётчик не найден: " + id));
        // Обновляем только разрешённые поля (номер, SIM, ICCID, статус)
        // Тип, фазность, ампер, значность, напряжение — только через справочник
        device.setMeterNumber(dto.getMeterNumber());
        device.setSimCardNumber(dto.getSimCardNumber());
        device.setIccidNumber(dto.getIccidNumber());
        device.setAvailable(dto.isAvailable());
        return toDTO(meterDeviceRepository.save(device));
    }

    public void delete(Long id) {
        meterDeviceRepository.deleteById(id);
    }

    public MeterDeviceDTO toDTO(MeterDevice m) {
        MeterDeviceDTO dto = new MeterDeviceDTO();
        dto.setId(m.getId());
        dto.setMeterNumber(m.getMeterNumber());
        dto.setMeterType(m.getMeterType());
        dto.setSimCardNumber(m.getSimCardNumber());
        dto.setIccidNumber(m.getIccidNumber());
        dto.setSealNumber(m.getSealNumber());
        dto.setPhases(m.getPhases());
        dto.setAmperage(m.getAmperage());
        dto.setZnch(m.getZnch());
        dto.setVoltage(m.getVoltage());
        dto.setAvailable(m.isAvailable());
        // Данные из справочника 1С
        if (m.getMeterModel() != null) {
            dto.setMeterModelCode(m.getMeterModel().getCode());
            dto.setMeterModelName(m.getMeterModel().getName());
        }
        return dto;
    }
    /** Найти сущность MeterDevice по номеру (для mark-busy) */
    public java.util.Optional<com.metertracking.entity.MeterDevice> findEntityByNumber(String meterNumber) {
        return meterDeviceRepository.findByMeterNumber(meterNumber);
    }

    /** Сохранить сущность напрямую (для mark-busy) */
    public void saveEntity(com.metertracking.entity.MeterDevice device) {
        meterDeviceRepository.save(device);
    }

}
