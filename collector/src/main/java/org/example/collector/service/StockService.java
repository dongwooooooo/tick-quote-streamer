package org.example.collector.service;

import lombok.RequiredArgsConstructor;
import org.example.collector.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<String> getTargetStockCodes(List<String> stockNames) {
        return stockRepository.findAllByStockNameIsIn(stockNames);
    }
}
