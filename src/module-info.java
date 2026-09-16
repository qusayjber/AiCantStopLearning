module AiCantStopLearning {
    requires java.sql;
    requires java.net.http;
    requires java.desktop;

    requires javafx.controls;
    requires javafx.graphics;        // ← أضف هذا السطر
    requires javafx.base;            // ← وأضف هذا للاحتياط

    requires org.xerial.sqlitejdbc;
    requires org.slf4j;

    opens app to javafx.graphics;
}