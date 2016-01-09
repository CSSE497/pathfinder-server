package io.pathfinder.models;

import com.avaje.ebean.Model;

import java.security.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

@Entity public class CapacityParameter extends Model {

    public static final Find<String, CapacityParameter> find =
        new Find<String, CapacityParameter>() {
        };

    @Id @NotNull @GeneratedValue(strategy = GenerationType.IDENTITY) public long id;
    @ManyToOne @NotNull public Application application;
    @NotNull public String parameter;
    @Version public Timestamp lastUpdate;
}
