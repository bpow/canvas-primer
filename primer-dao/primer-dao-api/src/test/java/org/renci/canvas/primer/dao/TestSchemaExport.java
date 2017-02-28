package org.renci.canvas.primer.dao;

import java.io.File;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.Test;

public class TestSchemaExport {

    @Test
    public void test() {

        MetadataSources metadata = new MetadataSources(
                new StandardServiceRegistryBuilder().applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect").build());

        // grabbed from the persistence.xml file
        metadata.addAnnotatedClass(org.renci.canvas.primer.dao.model.NamedEntity.class);
        metadata.addAnnotatedClass(org.renci.canvas.primer.dao.model.LoadedSequence.class);
        metadata.addAnnotatedClass(org.renci.canvas.primer.dao.model.LoadedMetadata.class);
        metadata.addAnnotatedClass(org.renci.canvas.primer.dao.model.DatabaseLoaded.class);
        metadata.addAnnotatedClass(org.renci.canvas.primer.dao.model.Mappings.class);
        metadata.addAnnotatedClass(org.renci.canvas.primer.dao.model.MappingsDatabaseLoaded.class);
        metadata.addAnnotatedClass(org.renci.canvas.primer.dao.model.Status.class);

        File output = new File("target", "database.sql");
        if (output.exists()) {
            output.delete();
        }
        new SchemaExport().setDelimiter(";").setOutputFile(output.getAbsolutePath()).createOnly(EnumSet.of(TargetType.SCRIPT),
                metadata.buildMetadata());

    }

}
