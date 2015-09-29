package io.pathfinder.models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity(name = "Commodity")
public class Commodity extends Model {

  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public long id;


  @Column(name = "startLatitude", nullable = false)
  public double startLatitude;

  @Column(name = "startLongitude", nullable = false)
  public double startLongitude;

  @Column(name = "endLatitude", nullable = false)
  public double endLatitude;

  @Column(name = "endLongitude", nullable = false)
  public double endLongitude;

  @JsonIgnore
  @ManyToOne
  public Cluster parent;

  @Column(name = "param")
  public int param;

  public static Find<Long, Commodity> find = new Find<Long, Commodity>() {};
}
