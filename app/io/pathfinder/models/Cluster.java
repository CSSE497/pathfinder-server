package io.pathfinder.models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;

@Entity(name = "Cluster")
public class Cluster extends Model {

  @Id
  @Column(nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public long id;

  @JsonIgnore
  @ManyToOne
  public Cluster parent;

  @OneToMany(mappedBy="parent", cascade = CascadeType.ALL)
  public List<Cluster> subClusters;

  @OneToMany(cascade = CascadeType.ALL)
  public List<Vehicle> vehicles;

  @OneToMany(cascade = CascadeType.ALL)
  public List<Commodity> commodities;

  public static Find<Long, Cluster> find = new Find<Long, Cluster>(){};
}