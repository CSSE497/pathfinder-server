package io.pathfinder.models;

import com.avaje.ebean.Model;
import javax.persistence.*;

/**
 * Created by Carter on 9/17/2015.
 */
@Entity(name = "Commodity")
public class Commodity extends Model {

  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.AUTO)
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
}
