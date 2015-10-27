import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by yudun on 15/10/23.
 */
public class Q2 {
    static String resultHeader = ConfigSingleton.TEAMID + "," +
            ConfigSingleton.TEAM_AWS_ACCOUNT_ID + "\n";

    static ConfigSingleton config = ConfigSingleton.getInstance();

    public static void lookupMysql(String userid, String tweet_time, RoutingContext routingContext){

        config.mysqlClient.getConnection(res -> {
            if (res.succeeded()) {
                final SQLConnection connection = res.result();
                connection.query("SELECT tid, score, content" +
                        " FROM " + config.Q2TableName +
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
            } else {
//                 Failed to get connection - deal with it
                routingContext.response()
                        .putHeader("Connection", "keep-alive")
                        .putHeader("Content-Type", "text/plain;charset=UTF-8")
                        .end(resultHeader);
            }

            res.result().close();
        });
    }


    public static void lookupHbase(String userid, String tweet_time, RoutingContext routingContext){
        StringBuilder result = new StringBuilder(resultHeader);


        Get query = new Get(Bytes.toBytes(userid + "+" + tweet_time.replace(" ", "+")));
        query.addFamily(Bytes.toBytes(config.HbaseQ2FamilyName));

        try {
            query.setMaxVersions(3);
            Result res = config.tweetTable.get(query);

            result.append(Bytes.toString(res.getValue(Bytes.toBytes(config.HbaseQ2FamilyName),
                    Bytes.toBytes(config.HbaseQ2ColumnName))) + "\n");

            routingContext.response()
                    .putHeader("Connection", "keep-alive")
                    .putHeader("Content-Type", "text/plain;charset=UTF-8")
                    .end(result.toString());

        } catch (IOException e) {
            e.printStackTrace();
            routingContext.response()
                    .putHeader("Connection", "keep-alive")
                    .putHeader("Content-Type", "text/plain;charset=UTF-8")
                    .end(resultHeader);
        }

    }
}
