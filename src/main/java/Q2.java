import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by yudun on 15/10/23.
 */
public class Q2 {
    static String resultHeader = ConfigSingleton.TEAMID + "," +
            ConfigSingleton.TEAM_AWS_ACCOUNT_ID + "\n";

    static ConfigSingleton config = ConfigSingleton.getInstance();

    public static void lookup(SQLConnection connection, String userid, String tweet_time, RoutingContext routingContext){

        connection.query("SELECT tid, score, content" +
                " FROM " + config.Q1TableName +
                " WHERE created_at=\'" + tweet_time + "\'" +
                " AND uid=\'" + userid + "\'", res2 -> {

            StringBuilder result = new StringBuilder(resultHeader);

            if (res2.succeeded()) {
//                        System.out.println("uid=" + userid + ":" + "time=" + tweet_time);
                List<JsonObject> rows = res2.result().getRows();

                //Sort numerically by Tweet ID in ascending order
                rows.sort((r1, r2) ->
                        ((new BigInteger(r1.getString("tid")))
                                .compareTo(new BigInteger(r2.getString("tid")))));

//                        System.out.println(rows);
                for (JsonObject row : rows) {
                    result.append(row.getString("tid") + ":" +
                            row.getInteger("score") + ":" +
                            row.getString("content") + "\n");

                }

            }

            routingContext.response()
                    .putHeader("Connection", "keep-alive")
                    .putHeader("Content-Type", "text/plain;charset=UTF-8")
                    .end(result.toString());

        });

    }
}
