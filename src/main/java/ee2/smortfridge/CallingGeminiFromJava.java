package ee2.smortfridge;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class CallingGeminiFromJava {
    public String generateResponse(String prompt){  //made it a main so I could call it and test it myself, but its not necessary
        //Client client = new Client(); //initialize the client
        Client client = Client.builder().apiKey("GEMINI_API_KEY").build(); //this initializes your client without having to specify the API key in cmd, but it's not as safe. Pls only do this if I give permission

        try{
            String model = "gemini-3.1-flash-lite-preview"; //select the model. You can try others but there's a chance you'll get ratelimited. Since the 3.1 preview is the lates one, it has the highest limit.


            GenerateContentResponse response = client.models.generateContent(model, prompt, null); //makes the API call. Null refers to config parameters like max number of tokens, but we don't need that type of settings
            //System.out.println("Response: " + response.text()); //print response
            return response.text();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage()); //prints error message. This way you can easily tell if you forgot to specify the API key
        }
        return null;
    }
}



