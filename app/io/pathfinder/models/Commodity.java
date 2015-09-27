package io.pathfinder.models;

import com.avaje.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

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

  @Column(name = "param")
  public int param;

  public static Finder<Long, Commodity> find = new Finder<Long, Commodity>(
      Long.class, Commodity.class
  );
}
