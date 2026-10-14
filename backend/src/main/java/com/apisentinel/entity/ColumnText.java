package com.apisentinel.entity;

// Text that comes from an uploaded file can be longer than its column. It is shortened to fit,
// because a value the database refuses fails the whole request with a server error.
final class ColumnText {

    private ColumnText() {
    }

    static String fit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        int end = maxLength;
        // Characters such as emoji are two Java chars; never keep only the first half.
        if (Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }
}
