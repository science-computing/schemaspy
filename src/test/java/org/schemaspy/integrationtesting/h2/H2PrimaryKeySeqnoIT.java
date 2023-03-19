/*
 * Copyright (C) 2017, 2018 Nils Petzaell
 *
 * This file is part of SchemaSpy.
 *
 * SchemaSpy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SchemaSpy. If not, see <http://www.gnu.org/licenses/>.
 */
package org.schemaspy.integrationtesting.h2;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.schemaspy.Config;
import org.schemaspy.cli.CommandLineArgumentParser;
import org.schemaspy.cli.CommandLineArguments;
import org.schemaspy.input.dbms.service.DatabaseServiceFactory;
import org.schemaspy.input.dbms.service.SqlService;
import org.schemaspy.model.Database;
import org.schemaspy.model.ProgressListener;
import org.schemaspy.model.TableColumn;
import org.schemaspy.testing.H2MemoryRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nils Petzaell
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class H2PrimaryKeySeqnoIT {

    @ClassRule
    public static H2MemoryRule h2MemoryRule = new H2MemoryRule("pkorder").addSqlScript("src/test/resources/integrationTesting/h2/dbScripts/pkordering.sql");

    @Autowired
    private SqlService sqlService;

    @Mock
    private ProgressListener progressListener;

    private static Database database;

    @Before
    public synchronized void createDatabaseRepresentation() throws SQLException, IOException {
        if (database == null) {
            doCreateDatabaseRepresentation();
        }
    }

    private void doCreateDatabaseRepresentation() throws SQLException, IOException {
        String[] args = {
            "-t", "src/test/resources/integrationTesting/dbTypes/h2memory",
            "-db", "pkorder",
            "-s", h2MemoryRule.getConnection().getSchema(),
            "-cat", h2MemoryRule.getConnection().getCatalog(),
            "-o", "target/testout/integrationtesting/h2/pkorder",
            "-u", "sa"
        };
        Config config = new Config(args);
        CommandLineArguments arguments = new CommandLineArgumentParser(
            new CommandLineArguments(),
            (option) -> null
        ).parse(args);
        sqlService.connect(arguments.getConnectionConfig());
        Database database = new Database(
            sqlService.getDbmsMeta(),
            arguments.getConnectionConfig().getDatabaseName(),
            arguments.getCatalog(),
            arguments.getSchema()
        );
        new DatabaseServiceFactory(sqlService).forSingleSchema(config).gatherSchemaDetails(database, null, progressListener);
        H2PrimaryKeySeqnoIT.database = database;
    }

    @Test
    public void primaryKeysShouldBeInCorrectOrder() {
        List<String> pkcolumnNames = database.getTablesMap().get("TABLE1").getPrimaryColumns().stream().map(TableColumn::getName).collect(Collectors.toList());
        assertThat(pkcolumnNames).containsExactly("ZZ", "AA", "BB");
    }
}
