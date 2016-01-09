package io.pathfinder.models;

import com.avaje.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity public class ObjectiveFunction extends Model {
    public static final Find<String, ObjectiveFunction> find =
        new Find<String, ObjectiveFunction>() {
        };

    @Id @NotNull public String id;
    @NotNull public String function;

}
