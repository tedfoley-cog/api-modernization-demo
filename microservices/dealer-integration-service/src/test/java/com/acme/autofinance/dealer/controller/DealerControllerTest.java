package com.acme.autofinance.dealer.controller;

import com.acme.autofinance.dealer.domain.DealPackage;
import com.acme.autofinance.dealer.domain.DealStatus;
import com.acme.autofinance.dealer.domain.Dealer;
import com.acme.autofinance.dealer.service.DealerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DealerController.class)
@Import(DealerExceptionHandler.class)
class DealerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DealerService dealerService;

    private DealPackage deal(Long id, Long dealerId) {
        DealPackage d = new DealPackage();
        d.setId(id);
        d.setDealerId(dealerId);
        d.setVehicleVin("1HGCM82633A004352");
        d.setSalePrice(new BigDecimal("32000.00"));
        d.setStatus(DealStatus.SUBMITTED);
        d.setDealNumber("DL-TEST0001");
        return d;
    }

    private Dealer dealer(Long id, String code) {
        Dealer dealer = new Dealer();
        dealer.setId(id);
        dealer.setDealerCode(code);
        dealer.setDealerName("Metro Auto Group");
        dealer.setActive(true);
        dealer.setTier("PLATINUM");
        return dealer;
    }

    @Test
    void submitDealPackageReturns201WithBody() throws Exception {
        DealPackage request = new DealPackage();
        request.setDealerId(1L);
        request.setVehicleVin("1HGCM82633A004352");
        request.setSalePrice(new BigDecimal("32000.00"));

        when(dealerService.submitDealPackage(any(DealPackage.class))).thenReturn(deal(1L, 1L));

        mockMvc.perform(post("/api/dealers/deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dealerId").value(1))
                .andExpect(jsonPath("$.dealNumber").value("DL-TEST0001"));
    }

    @Test
    void submitDealPackageRejectsMissingDealerId() throws Exception {
        DealPackage request = new DealPackage();
        request.setVehicleVin("1HGCM82633A004352");

        mockMvc.perform(post("/api/dealers/deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Dealer ID is required"));
    }

    @Test
    void submitDealPackageRejectsMissingVin() throws Exception {
        DealPackage request = new DealPackage();
        request.setDealerId(1L);

        mockMvc.perform(post("/api/dealers/deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Vehicle VIN is required"));
    }

    @Test
    void getDealerSettlementReturnsSummary() throws Exception {
        Map<String, Object> settlement = new HashMap<>();
        settlement.put("dealerCode", "DLR-001");
        settlement.put("totalReserves", new BigDecimal("918.75"));
        settlement.put("totalHoldbacks", new BigDecimal("1050.00"));
        settlement.put("netSettlement", new BigDecimal("-131.25"));
        when(dealerService.getDealerSettlement(eq(1L))).thenReturn(settlement);

        mockMvc.perform(get("/api/dealers/1/settlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealerCode").value("DLR-001"))
                .andExpect(jsonPath("$.totalReserves").value(918.75))
                .andExpect(jsonPath("$.netSettlement").value(-131.25));
    }

    @Test
    void getInventoryFinancingReturnsSummary() throws Exception {
        Map<String, Object> financing = new HashMap<>();
        financing.put("dealerCode", "DLR-001");
        financing.put("tier", "PLATINUM");
        financing.put("activeLoans", 2);
        financing.put("floorPlanLimit", new BigDecimal("5000000"));
        when(dealerService.getInventoryFinancing(eq(1L))).thenReturn(financing);

        mockMvc.perform(get("/api/dealers/1/inventory-financing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealerCode").value("DLR-001"))
                .andExpect(jsonPath("$.tier").value("PLATINUM"))
                .andExpect(jsonPath("$.activeLoans").value(2))
                .andExpect(jsonPath("$.floorPlanLimit").value(5000000));
    }

    @Test
    void listDealersReturnsList() throws Exception {
        when(dealerService.getAllDealers())
                .thenReturn(Arrays.asList(dealer(1L, "DLR-001"), dealer(2L, "DLR-002")));

        mockMvc.perform(get("/api/dealers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dealerCode").value("DLR-001"))
                .andExpect(jsonPath("$[1].dealerCode").value("DLR-002"));
    }
}
