package ee2.smortfridge;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class Connector {
    public Connector(){

    }
    public HttpResponse<String> makeGETRequest(HttpClient client, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
//    public void parseJSON(String jsoString) {
//        try {
//            JSONArray array = new JSONArray(jsoString);
//
//            for(int i = 0; i < array.length(); ++i) {
//                JSONObject o = array.getJSONObject(i);
//                int id = o.getInt("id");
//                String firstName = o.getString("firstName");
//                String lastName = o.getString("lastName");
//                System.out.println("ID: " + id + " | first name:" + firstName + " | last name:" + lastName);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
    public void main(String[] args) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = "https://studev.groept.be/api/a25ee2teachers/selectAllDummy";
            HttpResponse<String> response = this.makeGETRequest(client, url);
            System.out.println("Status Code: " + response.statusCode());
            //this.parseJSON(response.body());
            client.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
