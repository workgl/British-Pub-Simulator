import pub.*;
public class AudioTest {
    public static void main(String[] a) throws Exception {
        Audio.master = 0.15; Audio.speechOn = false;
        Audio.start();
        Thread.sleep(300);
        Audio.crowd = 0.6;
        for (String s : new String[]{"door","bell","till","pour","coin","ach","goal","whistle","groan","gasp","cheer","chant","shout","mic","guitar","static","powerdown","knock","flap","coo","plate","dart","pool","pot","wrong","right","murmur","song:0","song:4","song:6","song:11"}) { Audio.sfx(s); Thread.sleep(60); }
        Thread.sleep(2500);
        System.out.println("audio ok");
        System.exit(0);
    }
}
