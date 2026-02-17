package ru.itis.dis403.lab_02.orm;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ru.itis.dis403.lab_02.orm.annotation.Column;
import ru.itis.dis403.lab_02.orm.annotation.Id;
import ru.itis.dis403.lab_02.orm.annotation.ManyToOne;
import ru.itis.dis403.lab_02.orm.util.PathScan;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static ru.itis.dis403.lab_02.orm.util.SchemaUtil.getColumns;
import static ru.itis.dis403.lab_02.orm.util.SchemaUtil.getTables;

public class EntityManagerFactory {

    private HikariDataSource dataSource;
    private List<Class<?>> entities;
    private static final String PACKAGE = "ru.itis.dis403.lab_02.orm.model";

    public EntityManagerFactory() throws ClassNotFoundException {

        Class.forName("org.postgresql.Driver");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/lab2_02");
        config.setUsername("postgres");
        config.setPassword("postgres");
        config.setConnectionTimeout(50000);
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

        scanEntities();
        createTables();
    }

    public EntityManager getEntityManager() {
        try {
            return new EntityManagerImpl(dataSource.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        dataSource.close();
    }

    private void scanEntities() {
        entities = PathScan.find(PACKAGE);
    }

    private void createTables() {
        try (Connection connection = dataSource.getConnection()) {
            for (Class<?> entity : entities) {

                String sql = buildSQL(entity);

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.execute();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildSQL(Class<?> entity) {

        String tableName = entity.getSimpleName().toLowerCase();

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ")
           .append(tableName)
           .append(" (");

        Field[] fields = entity.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                sql.append(field.getName())
                   .append(" BIGSERIAL PRIMARY KEY,");
            }
            else if (field.isAnnotationPresent(Column.class)) {
                sql.append(field.getName())
                   .append(" VARCHAR(255),");
            }
            else if (field.isAnnotationPresent(ManyToOne.class)) {
                sql.append(field.getName())
                   .append("_id BIGINT,");
            }
        }

        if (sql.charAt(sql.length() - 1) == ',') {
            sql.deleteCharAt(sql.length() - 1);
        }
        sql.append(")");

        return sql.toString();
    }
}
