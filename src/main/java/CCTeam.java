import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
 * Created by yudun on 15/10/17.
 */
public class CCTeam extends AbstractVerticle {

    static ConfigSingleton config = null;

    Router router = Router.router(vertx);

    static ArrayList<BlockingQueue<Q2Request>> globalQueueArray = null;

    public void home(Route routerHome){
        routerHome.handler(routingContext -> {
            routingContext.response().end("<h1>Hello, We are " + config.TEAMID + "!" +
                    "<br> This is our CC team project home page</h1>");
        });
    }

    public void Q1(Route routerQ1){
        routerQ1.handler(routingContext -> {
            String key = routingContext.request().getParam("key");
            String message = routingContext.request().getParam("message");

            String result = Q1.decrypt(key, message);

            routingContext.response().end(result);
        });
    }

    public void Q2(SQLConnection connection, Route routerQ2){
        routerQ2.handler(routingContext -> {
            String userid = routingContext.request().getParam("userid");
            String tweet_time = routingContext.request().getParam("tweet_time");

            Q2.lookup(connection, userid, tweet_time, routingContext);
        });


    }

    public static void main(String[] args) {
        config = ConfigSingleton.getInstance();
        Runner.runExample(CCTeam.class);
        globalQueueArray = new ArrayList<BlockingQueue<Q2Request>>();
    }

    @Override
    public void start() throws Exception {
        JsonObject mysqlConfig = new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/" + config.Q1DBName)
                .put("driver_class", "com.mysql.jdbc.Driver")
                .put("user", config.mysqlUser)
                .put("password", config.mysqlPass)
                .put("max_pool_size", config.MAX_MYSQL_CONNECTION)
                .put("min_pool_size", config.MAX_MYSQL_CONNECTION)
                .put("initial_pool_size", config.MAX_MYSQL_CONNECTION);

        // initial the mysql Client
        config.mysqlClient = JDBCClient.createShared(vertx, mysqlConfig);



        // Start Testing our Q2 tester
//        Q2tester.start();

        Route routerHome = router.route("/");
        home(routerHome);

        Route routerQ1 = router.route("/q1");
        Q1(routerQ1);

        // open a single connection for all of our q2 requests
        config.mysqlClient.getConnection(res -> {
            if (res.succeeded()) {
                final SQLConnection connection = res.result();
                Route routerQ2 = router.route("/q2");
                Q2(connection, routerQ2);
            } else {
                // Failed to get connection - deal with it
            }
        });

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

}

