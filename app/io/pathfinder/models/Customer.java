package io.pathfinder.models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.security.Timestamp;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity public class Customer extends Model {

    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final Find<String, Customer> find = new Find<String, Customer>() {
    };

    @Id @NotNull public String email;
    @JsonIgnore @NotNull @Size(min = PASSWORD_MIN_LENGTH) public String password;
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL) public List<Application>
        applications;
    @Version public Timestamp lastUpdate;
}
