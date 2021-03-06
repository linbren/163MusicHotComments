import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dongliu.requests.Requests;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicAPI {

    private static String getParams(String param) {
        byte[] forth_param = {'0', 'C', 'o', 'J', 'U', 'm', '6', 'Q', 'y', 'w', '8', 'W', '8', 'j', 'u', 'd'};
        byte[] iv = {'0', '1', '0', '2', '0', '3', '0', '4', '0', '5', '0', '6', '0', '7', '0', '8'};
        byte[] secondKey = {'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F'};
        String h_encText;
        h_encText = Util.AES_encrypt(param, forth_param, iv);
        h_encText = Util.AES_encrypt(h_encText, secondKey, iv);
        return h_encText;
    }

    private static String getJson(String params, String url) {
        String encSecKey = "257348aecb5e556c066de214e531faadd1c55d814f9be95fd06d6bff9f4c7a41f831f6394d5a3fd2e3881736d94a02ca919d952872e7d0a50ebfa1769a7a62d512f5f1ca21aec60bc3819a9c3ffca5eca9a0dba6d6f7249b06f5965ecfff3695b54e1c28f3f624750ed39e7de08fc8493242e26dbc4484a01c76f739e135637c";
        Map<String, String> data = new HashMap<String, String>();
        data.put("params", params);
        data.put("encSecKey", encSecKey);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8");
        headers.put("Connection", "keep-alive");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Host", "music.163.com");
        headers.put("Origin", "http://music.163.com");
        headers.put("Referer", "http://music.163.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        return Requests.post(url).headers(headers).forms(data).send().readToText();
    }

    @SuppressWarnings("unchecked")
    private static List<Comments> getComment(String json, String songId) {
        Gson g = new Gson();
        Map<String, Object> map = g.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        List<Object> comments = (List<Object>) map.get("hotComments");
        List<Comments> list = new ArrayList<Comments>();
        Comments com;
        BigDecimal bd;
        for (Object o : comments) {
            Map<String, Object> tmp = (Map<String, Object>) o;
            com = new Comments();
            com.setSongId(songId);
            com.setContent(tmp.get("content").toString());
            com.setLikedCount(tmp.get("likedCount").toString().substring(0, tmp.get("likedCount").toString().length() - 2));
            bd = new BigDecimal(tmp.get("time").toString());
            com.setTime(bd.toPlainString());
            bd = new BigDecimal(((Map<String, Object>) tmp.get("user")).get("userId").toString());
            if (bd.toPlainString().contains(".")) {
                com.setUserId(bd.toPlainString().substring(0, bd.toPlainString().length() - 2));
            } else {
                com.setUserId(bd.toPlainString());
            }
            list.add(com);
        }
        if (!list.isEmpty()) {
            System.out.println("----> 获取" + songId + "的热门评论成功!");
        } else {
            System.out.println("----> 获取" + songId + "的热门评论失败!");
        }
        return list;
    }

    public static List<Comments> comments(String songId) {
        String url = "http://music.163.com//weapi/v1/resource/comments/R_SO_4_" + songId + "?csrf_token=";
        String param = "{rid:'', offset:'0', total:'true', limit:'20', csrf_token:''}";
        return getComment(getJson(getParams(param), url), songId);
    }

    @SuppressWarnings("unchecked")
    private static String getSinger(String json) {
        Gson g = new Gson();
        Map<String, Object> map = g.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        Map<String, Object> result = (Map<String, Object>) map.get("result");
        String singerId = ((Map<String, Object>) ((List<Object>) result.get("artists")).get(0)).get("id").toString();
        return singerId.substring(0, singerId.length() - 2);
    }

    public static String singer(String name) {
        String url = "http://music.163.com/weapi/search/suggest/web?csrf_token=";
        String param = "{s:'" + name + "', csrf_token:''}";
        String singId = getSinger(getJson(getParams(param), url));
        System.out.println(name + " ----> " + singId);
        return singId;
    }

    public static List<String> album(String singerId) {
        List<String> albumList = new ArrayList<String>();
        try {
            String url = "http://music.163.com/artist/album?id=" + singerId + "&limit=50";
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href].tit");
            for (Element link : links) {
                albumList.add(link.attr("href").substring(10));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Album Id: " + albumList);
        return albumList;
    }

    public static List<String> song(List<String> albumList) {
        List<String> songList = new ArrayList<String>();
        try {
            for (String album : albumList) {
                String url = "http://music.163.com/album?id=" + album;
                Document doc = Jsoup.connect(url).get();
                Elements links = doc.select("ul.f-hide a");
                for (Element link : links) {
                    songList.add(link.attr("href").substring(9));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Song Id : " + songList);
        return songList;
    }
}
