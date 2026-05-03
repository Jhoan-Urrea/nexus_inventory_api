package com.example.nexus.modules.inventory.util;

import com.example.nexus.exception.ValidationException;

/**
 * Validación y normalización de códigos de barras EAN-13 (GS1).
 * <p>
 * Dígito de control (posiciones 1-12 para datos, posición 13 = verificador):
 * {@code suma = 3 * sum(dígitos en posiciones pares) + sum(dígitos en posiciones impares)}
 * con posiciones 1-based desde la izquierda; verificador = {@code (10 - (suma % 10)) % 10}.
 */
public final class BarcodeValidator {

    public static final int EAN13_LENGTH = 13;
    public static final String MSG_INVALID_EAN13 = "Código de barras inválido. Debe cumplir formato EAN-13";
    public static final String MSG_BARCODE_REQUIRED = "El código de barras es obligatorio";

    private BarcodeValidator() {}

    /**
     * Elimina espacios y cualquier carácter no numérico.
     */
    public static String normalizeBarcode(String barcode) {
        if (barcode == null) {
            return null;
        }
        return barcode.replaceAll("\\s+", "").replaceAll("\\D", "");
    }

    /**
     * Valida estructura y dígito verificador EAN-13 (entrada normalizada o cruda).
     */
    public static boolean isValidEAN13(String barcode) {
        String digits = normalizeBarcode(barcode);
        if (digits == null || digits.length() != EAN13_LENGTH) {
            return false;
        }
        int expectedCheck = Character.getNumericValue(digits.charAt(EAN13_LENGTH - 1));
        return expectedCheck == computeCheckDigit(digits.substring(0, EAN13_LENGTH - 1));
    }

    /**
     * Normaliza, exige no vacío, formato EAN-13 y dígito verificador.
     *
     * @throws ValidationException si falta o no es EAN-13 válido
     */
    public static void assertBarcodeValid(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new ValidationException(MSG_BARCODE_REQUIRED);
        }
        String normalized = normalizeBarcode(barcode);
        if (normalized.isEmpty()) {
            throw new ValidationException(MSG_BARCODE_REQUIRED);
        }
        if (!isValidEAN13(normalized)) {
            throw new ValidationException(MSG_INVALID_EAN13);
        }
    }

    private static int computeCheckDigit(String twelveDigits) {
        if (twelveDigits == null || twelveDigits.length() != EAN13_LENGTH - 1) {
            throw new IllegalArgumentException("Se requieren exactamente 12 dígitos para el cálculo EAN-13");
        }
        int sumOddPositions = 0;
        int sumEvenPositions = 0;
        for (int i = 0; i < twelveDigits.length(); i++) {
            char c = twelveDigits.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Solo se permiten dígitos");
            }
            int d = c - '0';
            int positionOneBased = i + 1;
            if (positionOneBased % 2 == 1) {
                sumOddPositions += d;
            } else {
                sumEvenPositions += d;
            }
        }
        int total = 3 * sumEvenPositions + sumOddPositions;
        return (10 - (total % 10)) % 10;
    }
}
