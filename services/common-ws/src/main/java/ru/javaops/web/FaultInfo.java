package ru.javaops.web;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.javaops.masterjava.ExceptionType;

import javax.xml.bind.annotation.XmlType;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@XmlType(namespace = "http://common.javaops.ru/")
public class FaultInfo {
    @NonNull
    private ExceptionType type;

    @Override
    public String toString() {
        return type.toString();
    }
}
