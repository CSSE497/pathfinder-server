import javax.inject.Inject;

import play.api.http.HttpFilters;
import play.filters.cors.CORSFilter;

/**
 * Created by Carter on 9/21/2015.
 */
class Filters @Inject() (corsFilter: CORSFilter) extends HttpFilters {
  def filters = Seq(corsFilter);
}
