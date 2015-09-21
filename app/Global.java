import java.net.URL;

import play.*;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Created by Carter on 9/20/2015.
 *
 * Set up to allow cors
 *
 * From: http://stackoverflow.com/questions/25152277/play-framework-2-3-cors-headers
 */
public class Global extends GlobalSettings {

  // For CORS
  private class ActionWrapper extends Action.Simple {
    public ActionWrapper(Action<?> action) {
      this.delegate = action;
    }

    @Override
    public Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
      Promise<Result> result = this.delegate.call(ctx);
      Http.Response response = ctx.response();
      response.setHeader("Access-Control-Allow-Origin", "*");
      return result;
    }
  }

  @Override
  public Action<?> onRequest(Http.Request request, java.lang.reflect.Method actionMethod) {
    return new ActionWrapper(super.onRequest(request, actionMethod));
  }

}
