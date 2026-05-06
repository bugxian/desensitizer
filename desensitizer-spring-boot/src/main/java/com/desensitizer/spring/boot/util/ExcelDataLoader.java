package com.desensitizer.spring.boot.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(ExcelDataLoader.class);
    private static final String EXCEL_FILE = "test-data/赛题4-支持敏感信息脱敏的通用工具-测试数据v0.3.xlsx";

    public static class LogEntry {
        private String name;
        private String phone;
        private String idCard;
        private String bankCard;
        private String address;
        private String country;
        
        // 预期脱敏结果字段
        private String nameDesensitized;
        private String phoneDesensitized;
        private String idCardDesensitized;
        private String bankCardDesensitized;
        private String addressDesensitized;
        private String countryDesensitized;

        public LogEntry(String name, String phone, String idCard, String bankCard, String address, 
                       String country, String nameDesensitized, String phoneDesensitized,
                       String idCardDesensitized, String bankCardDesensitized, String addressDesensitized,
                       String countryDesensitized) {
            this.name = name;
            this.phone = phone;
            this.idCard = idCard;
            this.bankCard = bankCard;
            this.address = address;
            this.country = country;
            this.nameDesensitized = nameDesensitized;
            this.phoneDesensitized = phoneDesensitized;
            this.idCardDesensitized = idCardDesensitized;
            this.bankCardDesensitized = bankCardDesensitized;
            this.addressDesensitized = addressDesensitized;
            this.countryDesensitized = countryDesensitized;
        }

        public String toLogString() {
            StringBuilder sb = new StringBuilder();
            if (name != null && !name.isEmpty()) sb.append("姓名：").append(cleanValue(name)).append("，");
            if (phone != null && !phone.isEmpty()) sb.append("手机号：").append(cleanValue(phone)).append("，");
            if (idCard != null && !idCard.isEmpty()) sb.append("身份证号：").append(cleanValue(idCard)).append("，");
            if (bankCard != null && !bankCard.isEmpty()) sb.append("银行卡号：").append(cleanValue(bankCard)).append("，");
            if (address != null && !address.isEmpty()) sb.append("地址：").append(cleanValue(address));
            String result = sb.toString();
            if (result.endsWith("，")) {
                result = result.substring(0, result.length() - 1);
            }
            return result.trim();
        }
        
        private String cleanValue(String value) {
            if (value == null) return null;
            return value.trim().replaceAll("[\\s\\u00A0]+", " ");
        }

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getIdCard() { return idCard; }
        public String getBankCard() { return bankCard; }
        public String getAddress() { return address; }
        public String getCountry() { return country; }
        
        public String getNameDesensitized() { return nameDesensitized; }
        public String getPhoneDesensitized() { return phoneDesensitized; }
        public String getIdCardDesensitized() { return idCardDesensitized; }
        public String getBankCardDesensitized() { return bankCardDesensitized; }
        public String getAddressDesensitized() { return addressDesensitized; }
        public String getCountryDesensitized() { return countryDesensitized; }
    }

    public List<LogEntry> loadFromExcel() {
        List<LogEntry> entries = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource(EXCEL_FILE);
            if (!resource.exists()) {
                logger.warn("Excel file not found: {}", EXCEL_FILE);
                return entries;
            }

            try (InputStream is = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(is)) {

                Sheet sheet = workbook.getSheetAt(0);
                int rowCount = sheet.getPhysicalNumberOfRows();
                
                logger.info("Loading {} rows from Excel file", rowCount);

                // 从第3行开始读取（索引为2，第1行是标题，第2行是表头）
                for (int i = 2; i < rowCount; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    // 读取原始数据列（1-6列）
                    String name = getCellValueAsString(row.getCell(0));
                    String phone = getCellValueAsString(row.getCell(1));
                    String idCard = getCellValueAsString(row.getCell(2));
                    String bankCard = getCellValueAsString(row.getCell(3));
                    String address = getCellValueAsString(row.getCell(4));
                    String country = getCellValueAsString(row.getCell(5));
                    
                    // 读取预期脱敏结果列（7-12列）
                    String nameDesensitized = getCellValueAsString(row.getCell(6));
                    String phoneDesensitized = getCellValueAsString(row.getCell(7));
                    String idCardDesensitized = getCellValueAsString(row.getCell(8));
                    String bankCardDesensitized = getCellValueAsString(row.getCell(9));
                    String addressDesensitized = getCellValueAsString(row.getCell(10));
                    String countryDesensitized = getCellValueAsString(row.getCell(11));

                    // 跳过空行
                    boolean allEmpty = isEmpty(name) && isEmpty(phone) && isEmpty(idCard) 
                                     && isEmpty(bankCard) && isEmpty(address) && isEmpty(country);
                    if (allEmpty) {
                        continue;
                    }

                    entries.add(new LogEntry(name, phone, idCard, bankCard, address, country,
                                            nameDesensitized, phoneDesensitized, idCardDesensitized,
                                            bankCardDesensitized, addressDesensitized, countryDesensitized));
                }

                logger.info("Successfully loaded {} log entries from Excel with expected desensitized values", entries.size());
            }
        } catch (Exception e) {
            logger.error("Error loading Excel file: {}", e.getMessage(), e);
        }
        return entries;
    }
    
    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    }
                    return String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }
}
