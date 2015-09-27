package io.pathfinder.models;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.List;

@Entity(name = "Cluster")
public class Cluster extends Model {

  @Id
  @Column(nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int id;

  @ManyToOne
  public Cluster parent;

  @OneToMany(mappedBy="parent")
  public List<Cluster> subClusters;

  @OneToMany(cascade = CascadeType.ALL)
  public List<Vehicle> vehicles;

  @OneToMany(cascade = CascadeType.ALL)
  public List<Commodity> commodities;

}