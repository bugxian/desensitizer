package com.desensitizer.test;

import com.desensitizer.core.api.Desensitizer;
import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.builtin.desensitizer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class BuiltinDesensitizersTest {

    private DesensitizerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DesensitizerRegistry();
        registry.register(SensitiveType.PHONE, new PhoneDesensitizer());
        registry.register(SensitiveType.ID_CARD, new IdCardDesensitizer());
        registry.register(SensitiveType.BANK_CARD, new BankCardDesensitizer());
        registry.register(SensitiveType.EMAIL, new EmailDesensitizer());
        registry.register(SensitiveType.PASSWORD, new PasswordDesensitizer());
        registry.register(SensitiveType.ADDRESS, new AddressDesensitizer());
    }

    @ParameterizedTest
    @CsvSource({
            "13812345678, 138****5678",
            "13987654321, 139****4321",
            "18812345678, 188****5678",
            "15912345678, 159****5678"
    })
    void testPhoneDesensitizer_11Digits(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.PHONE);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "138123456, 138****56",
            "13812345, 138****45"
    })
    void testPhoneDesensitizer_lessThan11Digits(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.PHONE);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @Test
    void testPhoneDesensitizer_lessThan7Digits_noDesensitize() {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.PHONE);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize("1381234")).isEqualTo("138****34");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testPhoneDesensitizer_nullAndEmpty(String input) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.PHONE);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isNullOrEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "abc123def456"})
    void testPhoneDesensitizer_invalidFormat(String input) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.PHONE);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(input);
    }

    @ParameterizedTest
    @CsvSource({
            "110101199001011234, 110101********1234",
            "11010119900101123X, 110101********123X",
            "11010119900101123x, 110101********123x"
    })
    void testIdCardDesensitizer_18Digits(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.ID_CARD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "110101910101123, 110101******123"
    })
    void testIdCardDesensitizer_15Digits(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.ID_CARD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @Test
    void testIdCardDesensitizer_lessThan15Digits_noDesensitize() {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.ID_CARD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize("110101910101")).isEqualTo("110101910101");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIdCardDesensitizer_nullAndEmpty(String input) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.ID_CARD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isNullOrEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "6222021234567890123, 622202********0123",
            "622202123456789012, 622202********9012",
            "6217891234567890123, 621789********0123"
    })
    void testBankCardDesensitizer_19Digits(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.BANK_CARD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "6222021234567890, 622202********7890",
            "622202123456789, 622202123456789"
    })
    void testBankCardDesensitizer_16Digits(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.BANK_CARD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @Test
    void testBankCardDesensitizer_lessThan16Digits_noDesensitize() {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.BANK_CARD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize("622202123456")).isEqualTo("622202123456");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testBankCardDesensitizer_nullAndEmpty(String input) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.BANK_CARD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isNullOrEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "test@example.com, t***@example.com",
            "user123@company.cn, u***@company.cn"
    })
    void testEmailDesensitizer_standard(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.EMAIL);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "ab@example.com, **@example.com",
            "a@example.com, **@example.com"
    })
    void testEmailDesensitizer_shortUsername(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.EMAIL);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testEmailDesensitizer_nullAndEmpty(String input) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.EMAIL);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isNullOrEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "mySecretPassword, ****************",
            "anyPassword123, **************",
            "password, ********"
    })
    void testPasswordDesensitizer(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.PASSWORD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @Test
    void testPasswordDesensitizer_emptyString() {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.PASSWORD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize("")).isEqualTo("");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testPasswordDesensitizer_nullAndEmpty(String input) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.PASSWORD);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isNullOrEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "北京市朝阳区某某路123号, 北京市朝阳区***",
            "上海市浦东新区某街道456号, 上海市浦东新区***"
    })
    void testAddressDesensitizer_standard(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.ADDRESS);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "北京市, 北京市",
            "北京市朝阳区, 北京市朝阳区"
    })
    void testAddressDesensitizer_shortAddress(String input, String expected) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.ADDRESS);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testAddressDesensitizer_nullAndEmpty(String input) {
        Desensitizer desensitizer = registry.getDesensitizer(SensitiveType.ADDRESS);
        assertThat(desensitizer).isNotNull();
        assertThat(desensitizer.desensitize(input)).isNullOrEmpty();
    }
}
