package com.example.demo.securesql.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OracleValidatorTest {

    @Test
    void singleTable_noPrefix_ok() {
        String sql = "SELECT STORE_ID, PRODUCT_ID FROM SALES_TRANSACTION";
        assertDoesNotThrow(() -> OracleValidator.validate(sql));
    }

    @Test
    void multiTable_prefixMandatory_ok() {
        String sql = "SELECT ST.STORE_ID, SM.STORE_NAME " +
                "FROM SALES_TRANSACTION ST JOIN STORE_MASTER SM ON ST.STORE_ID = SM.STORE_ID";
        assertDoesNotThrow(() -> OracleValidator.validate(sql));
    }

    @Test
    void multiTable_missingPrefix_fail() {
        String sql = "SELECT STORE_ID, STORE_NAME " +
                "FROM SALES_TRANSACTION ST JOIN STORE_MASTER SM ON ST.STORE_ID = SM.STORE_ID";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> OracleValidator.validate(sql));
        assertTrue(ex.getMessage().contains("prefix 없는 컬럼"));
    }

    @Test
    void disallowedFunction_fail() {
        String sql = "SELECT HACKFUNC(STORE_ID) FROM SALES_TRANSACTION";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> OracleValidator.validate(sql));
        assertTrue(ex.getMessage().contains("허용되지 않은 함수"));
    }

    @Test
    void allowedFunction_ok() {
        String sql = "SELECT NVL(STORE_ID, 'X') FROM SALES_TRANSACTION";
        assertDoesNotThrow(() -> OracleValidator.validate(sql));
    }
}
