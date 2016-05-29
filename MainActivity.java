package com.bluetooth.dev.lua_test;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


//import org.luaj.vm2.Globals;
import org.luaj.vm2.Lua;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.sax.TransformerHandler;


public class MainActivity extends ActionBarActivity {

    // Constante utilisé par la fonction d'affichage d'erreur
    // Voir pour remplacer par énumeration
    final static int ERROR_BLUETOOTH = 1;
    final static int ERROR_INTERNET = 2;
    // MODE D'AFFICHAGE SELON LE CONTENU CHARGEE
    final static int MODE_MOBILE = 1;
    final static int MODE_PC = 2;
    static int width;
    static int height;
    //    private String user_name;
    // Variables utilisé par la class tactos
    private static Context context;
    private static RelativeLayout mainLayout;
    private static MainActivity instance;
    public ArrayList<int[]> g_evenement;
    public TextView textPico; // voir a supprimer
    public Point positionD;
    public int pos; // voir a supprimer
    public ArrayList<ObjetEvent> listeObjet;
    LuaValue globals = JsePlatform.standardGlobals(); // Globals pour LUA
    String lua_path; // Chemin ou sont contenu les fichiers Lua
    // Etats des boutons (retiré suite a une fuite de memoire,a corriger)
    boolean lbutton_state;
    boolean rbutton_state;
    // Données pour mode compatibilité PC
    Rect Zone_PC = null;
    float ratio_x = 0;
    float ratio_y = 0;
    // Liste des contenus utilisés dans la salle
    LinkedList<TactosImage> ImgList = new LinkedList<>();
    LinkedList<TactosSound> SoundList = new LinkedList<>();
    LinkedList<TactosText> TextList = new LinkedList<>();
    LinkedList<TactosDoor> DoorList = new LinkedList<>();
    LinkedList<TactosIcon> IconList = new LinkedList<>();
    // GLOBALS INTERACTION
    ScheduledExecutorService timeleader;
    ServerInteraction g_managerInteraction = null;
    // Mode d'affichage (PC par default)
    int display_mode = MODE_PC;
    ArrayList<TactosColor> listColorsDownloaded = null; // liste de couleurs chargées pour détecter les images a l'écran
    XmlParser objectParser = new XmlParser(); // objet permettant de parser un fichier xml
    BTConnexion co; // Objet gérant la connexion Bluetooth
    BluetoothDevice device; // Objet contenant les informations de l'appareil Bluetooth
    GestureDetectorCompat gDetect; // Detecteur de geste
    boolean[] result; // Matrice de picot renvoyé au boitier
    boolean[] tacticon_current = new boolean[16]; // Contenu du tacticon rempli a l'execution
    int tacticon_current_id = 0; // ID du tacticon qui tourne actuellement
    int pointeur_count; // Nombre de doigts qui touche l'ecran
    boolean tacicon_running = false; // Si vrai,alors Tacticton actif.
    String current_room; // Arborecence de la salle actuelle
    public String User_Name;       // Nom Intertact de l'utilisateur
    int Room_ID;
    private int user_id;
    // Données de la room chargé
    private int room_width;
    private int room_height;

    public static TextView testText;
    private HorlogeLua horloge;
    //public static ArrayList<Bitmap> fusionDessein;
    public ArrayList<ObjetPorte> listeDoor;
    public static ImageView finalImg;
    public Vibrator vibre;
    //public static ArrayList<Integer> position = new ArrayList<Integer>();

    public int ratio_room_x;
    public boolean modePortrait;

    public static boolean vide_tab = false;

    private ArrayList<ObjetEvent> flagMouseEnter;
    private ArrayList<ObjetEvent> flagClick;
    private ArrayList<ObjetEvent> flagExit;
    public ArrayList<ImageView> imageDefectueuse; // test, a supprimer
    private LinkedHashMap<Integer, FormDetector> g_users_imageview;
    // Recupère des messages d'autres Thread afin d'interagir avec le thread principal
    static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            boolean progress = msg.getData().getBoolean("CNX_STATE");
            if (!progress) {
                MainActivity.getInstance().error_Show(ERROR_BLUETOOTH);
            }
// dom-            else {
            MainActivity.getInstance().init();
// Dom-            }
        }
    };

    public static Context getContext() {
        return context;
    }

    public static RelativeLayout getMainLayout() {
        return mainLayout;
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public int getRoom_width() {
        return room_width;
    }

    public int getRoom_height() {
        return room_height;
    }

    // retourne le mode d'affichage actuel ( voir constantes MODE_PC et MODE_MOBILE)
    public int getDisplay_mode() {
        return display_mode;
    }

    // Retourne l'arborence de la salle actuellement visité
    public String getCurrentRoom() {
        return current_room;
    }

    // Lance la connexion Bluetooth
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        instance = this;
        setContentView(R.layout.activity_main);
        gDetect = new GestureDetectorCompat(this, new GestureListener());
        System.setProperty("http.keepAlive", "false");

        // Definition des variables utilisé par Tactos (Layout + Taille Ecran)
        mainLayout = (RelativeLayout) findViewById(R.id.test);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        // Copie des fichiers locaux vers le cache (Ex : Tactos_Menu etc...)
        AssetManager assetManager = getAssets();
        DataProvider.copyAssets(assetManager, getCacheDir());

        TactosCore.Say("CHARGEMENT");

        User_Name = getIntent().getStringExtra("UserName");
        Room_ID = getIntent().getIntExtra("RoomID", 1000);
        /* ee */
        g_evenement = new ArrayList<int[]>();
        //textPico = new TextView(MainActivity.getContext());
        //textPico.setLayoutParams(new RelativeLayout.LayoutParams(300, 300));
        Display dis = MainActivity.getInstance().getWindowManager().getDefaultDisplay();
        Point p = new Point();
        dis.getSize(p);
        p.set(p.x - 300, p.y - 300);
        positionD = new Point();
        //textPico.setX((float) p.x);
        //textPico.setY((float) p.y);
        //MainActivity.getMainLayout().addView(textPico);
        //textPico.setText(" - - - - \n - - - - \n - - - - \n - - - - ");
        listeObjet = new ArrayList<ObjetEvent>();
        flagMouseEnter = new ArrayList<ObjetEvent>();
        flagClick = new ArrayList<ObjetEvent>();
        flagExit = new ArrayList<ObjetEvent>();
        //fusionDessein = new ArrayList<Bitmap>();
        listeDoor = new ArrayList<ObjetPorte>();

        if (vide_tab) { // si la tab de couleur est encore plein, on la vide
            DetecteurPixel.listeColor.clear();
            for (ColorPico ob : DetecteurPixel.listeColor) {
                DetecteurPixel.listeColor.remove(ob);
            }
        }

        vide_tab = true;


        finalImg = new ImageView(MainActivity.getContext());
        Display disMain = MainActivity.getInstance().getWindowManager().getDefaultDisplay();
        Point pMain = new Point();
        disMain.getSize(pMain);
        Bitmap principaleImg = Bitmap.createBitmap(pMain.x, pMain.y, Bitmap.Config.ARGB_8888);
        Canvas can = new Canvas(principaleImg);

        Bitmap fond = BitmapFactory.decodeResource(getResources(), R.drawable.fond_blanc);
        Bitmap fond2 = Bitmap.createScaledBitmap(fond, pMain.x, pMain.y, false);
        can.drawBitmap(fond2, 0, 0, null);
        finalImg.setImageBitmap(principaleImg);
        MainActivity.getMainLayout().addView(finalImg);

        testText = new TextView(MainActivity.getContext()); // test visuel des picos
        testText.setLayoutParams(new RelativeLayout.LayoutParams(300, 300));
        //testText.setBackgroundColor(Color.RED);
        //testText.setTextColor(Color.YELLOW);
        if(PreferenceManager.getDefaultSharedPreferences(this).getString("deviceOrientation", "Portrait").equals("Portrait")) {
            testText.setX(pMain.x - 350);
            testText.setY(pMain.y - 300);
        }
        else{
            testText.setX(1000);
            testText.setY(500);
        }
        MainActivity.getMainLayout().addView(testText);
        testText.setText("Test de la comm");
        testText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                for (ObjetEvent ob : listeObjet) {
                    if(ob.identification().equals("IMAGE"))
                        Log.e("IMAGE ", " "+ob.getID());
                }
            }
        });

        vibre = (Vibrator) getSystemService(VIBRATOR_SERVICE); // variable pour vibrer le hardware
        imageDefectueuse = new ArrayList<ImageView>(); // recuperation des eventuelle image beugger


        // Initialisation de la connexion Bluetooth
        device = getIntent().getParcelableExtra("Device");
        if (device != null) {
            co = new BTConnexion(device);
            co.connect();
        } else {
            Message mes = new Message();
            Bundle messageBundle = new Bundle();
            messageBundle.putBoolean("CNX_STATE", false);
            mes.setData(messageBundle);
            handler.handleMessage(mes);
        }


        //Redraw();

        /*ImageView test = new ImageView(MainActivity.getContext());
        test.setLayoutParams(new RelativeLayout.LayoutParams(200, 200));
        test.setColorFilter(Color.GREEN);
        test.setBackgroundColor(Color.GREEN);
        test.setX(100);
        test.setY(800);
        MainActivity.getMainLayout().addView(test);
        listeObjet.add(new ObjetImage(test, 100, 800, 200 + 100, 200 + 800, "mouseLeftUp", 2500));

        ImageView test2 = new ImageView(MainActivity.getContext());
        test2.setLayoutParams(new RelativeLayout.LayoutParams(200, 200));
        test2.setColorFilter(Color.rgb(40, 0, 70));
        test2.setBackgroundColor(Color.rgb(40, 0, 70));
        test2.setX(100);
        test2.setY(1100);
        MainActivity.getMainLayout().addView(test2);
        listeObjet.add(new ObjetImage(test2, 100, 1100, 200 + 100, 200 + 1100, "mouseExit", 2501));*/




    }

    // Gère la déconnexion Bluetooth et Interaction lorsque l'on quitte la salle ou l'application
    @Override
    protected void onDestroy() {
        System.out.println("onDestroy called");
        super.onDestroy();

        if(horloge != null)
            horloge.getTime().cancel(); // si un timer fonction, on l'arrete
        try {
            co.close(); // on ferme la connexion
        } catch (Exception ex) {
            Log.e("co.close", " fait planter la fin");
        }

        if (g_managerInteraction != null)
            g_managerInteraction.stopAll(); // s'il y a une interaction multi-utilisateurs, engage la procedure d'arret propre

        try {
            timeleader.shutdown(); // demande au gestionaire de thread qui controle l'affichage d'autres utilisateur de s'arreter
            while (!(timeleader.isShutdown())) // on attend la fin du thread.
            {

                // WAIT
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (g_users_imageview != null && g_users_imageview.size() > 0) // S'il y a des detecteurs d'utilisateurs affichés et détectés à l'écran
        {
            for (Map.Entry<Integer, FormDetector> entry : g_users_imageview.entrySet()) // boucle qui supprime proprement les utilisateurs présent
            {
                FormDetector detector_temp = entry.getValue(); // on met le detecteur courant dans une variable temporaire
                detector_temp.getA_image_bitmap().recycle(); // on recycle son immage Bitmap pour libérer la mémoire utilisée
            }
            g_users_imageview.clear(); // on supprime tout ce qu'il reste dans la listes de detecteur
        }
    }

    // Fonction d'initalisation de la salle (XML + Lua lu + lancement de l'intéraction)
    private void init() {
        // Recuperations des paramètres utilisateurs dans l'object sharedPrefs
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this); // charge les option choisit
        if (sharedPrefs.getString("deviceOrientation", "Portrait").equals("Portrait")) { // indique le sens de l'hardware
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            modePortrait = true;
            TactosCore.setModePortrait(modePortrait);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            modePortrait = false;
            TactosCore.setModePortrait(modePortrait);
        }
        if (sharedPrefs.getString("caseOrientation", "Portrait").equals("Portrait")) { // indique le sens des picos
            DetecteurPixel.setPicoPortrait(true);
        } else {
            DetecteurPixel.setPicoPortrait(false);
        }
        if (sharedPrefs.getBoolean("inversion", false)){ // indique le sens du boitier ( bouton vers le haut )
            DetecteurPixel.setInversion(true);
        }else {
            DetecteurPixel.setInversion(false);
        }
        if (sharedPrefs.getBoolean("mirroir", false)){ // indique si le boitier tactos est derriere le smartphone
            DetecteurPixel.setMirroir(true);
        }else{
            DetecteurPixel.setMirroir(false);
        }

        try { // indique la taille du detecteur de pixel
            int nbPic;
            nbPic = Integer.parseInt(sharedPrefs.getString("nbPixel", ""));
            if (nbPic % 4 == 1)
                nbPic += 3;
            else if (nbPic % 4 == 2)
                nbPic += 2;
            else if (nbPic % 4 == 3)
                nbPic += 1;
            DetecteurPixel.reglagePico(nbPic);
        } catch (NumberFormatException e) {
            int nbPic;
            nbPic = 12;
            DetecteurPixel.reglagePico(nbPic);
        }

        DetecteurPixel.setPriorite(true);

        ProgressBar tmp_loading = (ProgressBar) findViewById(R.id.progressBar);
        InputStream in_s = null;
        tmp_loading.setVisibility(View.GONE);
        // verif dossier cache salles
        File sallesCacheDirectory = new File(getCacheDir(), "salles");
        if (!(sallesCacheDirectory.exists())) {
            sallesCacheDirectory.mkdirs();
            sallesCacheDirectory.setReadable(true);
        }

        // verif dossier cache tactosres
        File TactosResCacheDirectory = new File(getCacheDir(), "tactosres");
        if (!(TactosResCacheDirectory.exists())) {
            TactosResCacheDirectory.mkdirs();
            TactosResCacheDirectory.setReadable(true);
        }

        // A SUPPRIMER APRES INTEGRATION SALLE DL ETC..
        ZipHandler v_zipmanager = new ZipHandler();
        try {
            v_zipmanager.copyTo(getAssets().open("YOLO.xml"), getCacheDir().getAbsolutePath(), "Yolo.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            listColorsDownloaded = objectParser.parseColors(getCacheDir().getAbsolutePath() + "/Yolo.xml");
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        XmlParser.setA_colonamesfile(getCacheDir().getAbsolutePath() + "/color-names.xml");

        // Condition vraie lors du lancement de l'application (redirection auto vers lobby_v4)
// Dom --        if (getIntent().getStringExtra("idRoom") == null) {
/*Dom ++ */
        if (Room_ID == 0) {
// Dom ICI -            downloadRoom("lobby_v4");
            downloadRoom("game_menu_v4");
            File tmp_file = null;
            try {
                tmp_file = new File(current_room + "/" + current_room.split("/")[current_room.split("/").length - 1] + ".xml");
            }catch (NullPointerException nil){
                nil.printStackTrace();
                error_Show(ERROR_INTERNET);
                return;
            }

            try {
                in_s = new FileInputStream(tmp_file);
            } catch (IOException e) {
                e.printStackTrace();
            }


            // Recuperation des données de la salle contenu dans le XML
            XmlParser.RoomData room_data = XmlParser.getRoomXML_BETA(in_s);
            System.out.println(room_data.name);
            System.out.println(room_data.height);
            System.out.println(room_data.width);
            System.out.println(room_data.allow_caress);
            System.out.println(room_data.is_multiuser);
            System.out.println(room_data.sensibilty);
            System.out.println(room_data.script);

            // Si un script est indiqué dans le XML alors on l'execute
            if (room_data.script != null & room_data.script != "") {
                // Converti le fichier Lua d'origine vers une version mobile
                convertLua(room_data.script);

                // Initialisation et execution LUA
                PackageLib pk = new PackageLib();

                // Indique ou l'interpreter Lua va chercher les scripts Lua passé en require
                pk.DEFAULT_LUA_PATH = getCacheDir().getAbsolutePath() + "/?.lua";
                globals.load(pk);
                lua_path = getCacheDir().getAbsolutePath();

                // Charge le script de la salle
                try {
                    globals.get("dofile").call(LuaValue.valueOf(current_room + "/Scripts/" + room_data.script.split("\\.")[0] + "_mobile.lua"));
                    globals.get("onInit").call();
                } catch (LuaError err) {
                    err.printStackTrace();
                }

                // lance le systeme d'interaction multi-utilisateurs
                initInteraction(Integer.toString(user_id));
            }
        } else // Si un idRoom est passé en intent alors lance la salle passé en intent
        {
//Dom --            downloadRoom(getIntent().getStringExtra("idRoom"));
/*++*/
            ServerGetRoomName getRoomName = new ServerGetRoomName(Integer.toString(Room_ID));
/*++*/
            getRoomName.start();
/*++*/
            try {
/*++*/
                getRoomName.join();
/*++*/
            } catch (InterruptedException e) {
/*++*/
                e.printStackTrace();
/*++*/
            }

// /*++*/      downloadRoom(Integer.toString(Room_ID));
/*++*/
            downloadRoom(getRoomName.getA_room_name());
            if (current_room != null) {

                File tmp_file = new File(current_room + "/" + current_room.split("/")[current_room.split("/").length - 1] + ".xml");

                try {
                    in_s = new FileInputStream(tmp_file);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                XmlParser.RoomData tmp = XmlParser.getRoomXML_BETA(in_s);
                if (tmp != null) {
                    room_height = tmp.getHeight();
                    room_width = tmp.getWidth();
                    System.out.println(tmp.name);
                    System.out.println(tmp.height);
                    System.out.println(tmp.width);
                    System.out.println(tmp.allow_caress);
                    System.out.println(tmp.is_multiuser);
                    System.out.println(tmp.sensibilty);
                    System.out.println(tmp.script);

                    if (tmp.script != null && tmp.script != "") {
                        try {
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(current_room + "/Scripts/" + tmp.script)), "ISO-8859-1"));
                            String lecture = "";
                            while ((lecture = bufferedReader.readLine()) != null) {
                                Log.e("ligne ", " " + lecture);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        convertLua(tmp.script); // convertit le script pour le rendre lisible par java

                        // Initialisation et execution LUA
                        PackageLib pk = new PackageLib();
                        BaseLib bl = new BaseLib();
                        pk.DEFAULT_LUA_PATH = getCacheDir().getAbsolutePath() + "/?.lua";
                        globals.load(pk);
                        globals.load(bl);// ajout de library//
                        lua_path = getCacheDir().getAbsolutePath();
                        try {
                            globals.get("dofile").call(LuaValue.valueOf(current_room + "/Scripts/" + tmp.script.split("\\.")[0] + "_mobile.lua")); // ajout du script
                            GestionD.luaOnEvent = globals; // exporte le script dans la classe GestionD
                            LuaValue res; // variable pour le retour de onInit
                            int time = -1; // variable pour le retour de onInit
                            res = globals.get("onInit").call(); // appelle de la fonction onInit dans le script si elle existe
                            try {
                                time = Integer.parseInt(res.toString()); // conversion luaValue en int
                            } catch (NumberFormatException num) {
                                num.printStackTrace();
                            }
                            if (time > 0) {
                                horloge = new HorlogeLua(time, globals); // si time > 0, on demarre un timer
                            }
                        } catch (LuaError error) {
                            error.printStackTrace();
                        }

                    }

                }


// DOM --                // lance le systeme d'interaction multi-utilisateurs
// DOM --                initInteraction(Integer.toString(user_id));
                else {
                    Log.d("serveur", "probleme connexion");
                    error_Show(ERROR_INTERNET);
                }
            } else {
                Log.d("erreur", "wifi non utiliser");
                error_Show(ERROR_INTERNET);
            }
/*DOM++*/            // lance le systeme d'interaction multi-utilisateurs
/*DOM++*/   // 2016_04_01 -- pb serveur         initInteraction(Integer.toString(user_id));

        }

    }

    public void runThread(final String p_info) { // modifie la text view qui represente les picos
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                testText.setText(p_info);
            }
        }));
    }

    public void runRedraw(final ImageView img, final  Bitmap img2){ // fonction permettant a des threads secondaires de modifier les views
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                getMainLayout().removeView(img);
                img.setImageBitmap(img2);
                getMainLayout().addView(img);
                if(testText != null) {
                    getMainLayout().removeView(testText);
                    getMainLayout().addView(testText);
                }
            }
        }));
    }


    public void runOnEvent(ObjetEvent ob) {
        runOnUiThread(new Thread(new GestionD(ob)));
    } // si on active un evenement on l'execute par un thread

    public void onVibrate() { // permet de faire vibrer l'hardware
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                if (vibre.hasVibrator())
                    vibre.vibrate(100);
            }
        }));
    }

    // Initialisation de l'interaction entre plusieurs users dans la même salle appelé ci-dessus
    private void initInteraction(String userID) {
        // declaration d'un frequenceur de thread, qui ne peut contenir d'un thread
        timeleader = Executors.newScheduledThreadPool(1);

        // intialisation de la liste de detecteurs d'utilisateurs
        g_users_imageview = new LinkedHashMap<>();

        // recuperation de l'id de la salle courante
        ServerGetRoomID getRoomID = new ServerGetRoomID(current_room.split("/")[current_room.split("/").length - 1]);
        getRoomID.start();
        try {
            getRoomID.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // demande l'interaction multi-utilisateur au serveur
        ServerBeginInteraction v_AskForInteraction = new ServerBeginInteraction(userID, getRoomID.getA_room_id());
        v_AskForInteraction.start();
        try {
            v_AskForInteraction.join();

            // créer l'interaction avec le port recu precedemment
            g_managerInteraction = new ServerInteraction(v_AskForInteraction.getA_port(), user_id, ratio_x, ratio_y);
            g_managerInteraction.startAll(); // demarrage des threads


            // Thread cadencé qui gere l'affichage des utilisateurs à l'écran
            Runnable display_multi_user = new Runnable() {
                @Override
                public void run() {

                    // liste d'utilisateurs en ligne
                    LinkedList<Integer> v_current_users = new LinkedList<>();

                    // si les ratios sont bien en place
                    if (g_managerInteraction.getRatio_height() > 0) {
                        try {
                            g_managerInteraction.getSem().acquire(); // appel du semaphore de la liste d'utilisateurs pour eviter la concurrence
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (g_managerInteraction.getUsers() != null && g_managerInteraction.getUsers().size() > 0) // verifie si la liste d'utilisateurs n'est pas vide
                        {
                            User user_temp;
                            Iterator it_users; // iterateur

                            it_users = g_managerInteraction.getUsers().iterator();
                            while (it_users.hasNext()) // parcours de la liste d'utilisateurs
                            {
                                if (g_users_imageview == null)
                                    g_users_imageview = new LinkedHashMap<>();

                                user_temp = (User) it_users.next();
                                if (user_temp.isA_online()) // si l'utilisateur est en ligne
                                {
                                    v_current_users.add((Integer) user_temp.getA_id());

                                    final Semaphore local_sem = new Semaphore(0);
                                    if (g_users_imageview.containsKey((Integer) user_temp.getA_id())) // si il est deja affiché a l'écran on le met a jour (position etc...)
                                    {

                                        final FormDetector detector_temp = g_users_imageview.get((Integer) user_temp.getA_id());
                                        final User finalUser_temp = user_temp;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                float x, y;
                                                x = finalUser_temp.getA_posx() + Zone_PC.left;
                                                y = finalUser_temp.getA_posy() + Zone_PC.top;
                                                detector_temp.getA_image_view().setX(x);
                                                detector_temp.getA_image_view().setY(y);
                                                detector_temp.getA_image_view().setVisibility(View.VISIBLE);
                                                local_sem.release();
                                            }
                                        });

                                        try {
                                            local_sem.acquire();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        // mise à jour du decalage de l'avatar de l'utilisateur par rapport aux bords de l'écran
                                        int[] offset_avatar = new int[2];
                                        detector_temp.getA_image_view().getLocationOnScreen(offset_avatar);
                                        detector_temp.setA_decalage_x(offset_avatar[0]);
                                        detector_temp.setA_decalage_y(offset_avatar[1]);
                                    } else { // si l'utilisateur n'est pas encore affiché a l'écran, mais en ligne

                                        BitmapFactory.Options option = new BitmapFactory.Options();
                                        option.inScaled = false; // pas de mise a l'échelle du bitmap, on laisse la taille intiale
                                        Bitmap avatar = BitmapFactory.decodeResource(getResources(), R.drawable.black20, option);

                                        // mise en place des dimensions de l'avatar en mettant a l'échelle du smartphone
                                        float xdim = avatar.getWidth() * g_managerInteraction.getRatio_width();
                                        float ydim = avatar.getHeight() * g_managerInteraction.getRatio_height();
                                        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (xdim), (int) (ydim));
                                        params.setMargins(0, 0, 0, 0);

                                        final ImageView user_img_view = new ImageView(getApplicationContext());
                                        user_img_view.setImageResource(R.drawable.black20);
                                        user_img_view.setAdjustViewBounds(true);


                                        final User finalUser_temp1 = user_temp;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                MainActivity.getMainLayout().addView(user_img_view, params);
                                                float x, y;
                                                x = finalUser_temp1.getA_posx() + Zone_PC.left;
                                                y = finalUser_temp1.getA_posy() + Zone_PC.top;

                                                user_img_view.setX(x);
                                                user_img_view.setY(y);
                                                local_sem.release();
                                            }
                                        });
                                        try {
                                            local_sem.acquire();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        //ajout de l'utilisateur affiché avec le form detector
                                        g_users_imageview.put(user_temp.getA_id(), new FormDetector(avatar, user_img_view, null, 6, listColorsDownloaded));
                                    }
                                }
                            }

                            // #### DELETE ALL the offline users
                            Iterator it = g_managerInteraction.getUsers().iterator();

                            while (it.hasNext()) {
                                user_temp = (User) it.next();
                                if (!(user_temp.isA_online())) {
                                    final Semaphore sem_deleteuser = new Semaphore(0);
                                    final User finalUser_temp2 = user_temp;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            g_users_imageview.get(finalUser_temp2.getA_id()).getA_image_view().setVisibility(View.GONE);
                                            MainActivity.getMainLayout().removeView(g_users_imageview.get(finalUser_temp2.getA_id()).getA_image_view());
                                            sem_deleteuser.release();
                                        }
                                    });
                                    try {
                                        sem_deleteuser.acquire();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    g_users_imageview.get(user_temp.getA_id()).getA_image_bitmap().recycle();
                                    g_users_imageview.remove(user_temp.getA_id());
                                    g_managerInteraction.getUsers().remove(user_temp);
                                }
                            }
                        } else { // si la liste d'utilisateurs recue par les trames est vide, on supprime tout ce qu'on a comme detecteurs d'utilisateurs
                            // DESTROY THE DETECTORS
                            if (g_users_imageview != null && g_users_imageview.size() > 0) {
                                for (Map.Entry<Integer, FormDetector> entry : g_users_imageview.entrySet()) {
                                    final FormDetector detector_temp = entry.getValue();
                                    final Semaphore sem_deleteuser = new Semaphore(0);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            detector_temp.getA_image_view().setVisibility(View.GONE);
                                            MainActivity.getMainLayout().removeView(detector_temp.getA_image_view());
                                            sem_deleteuser.release();
                                        }
                                    });
                                    try {
                                        sem_deleteuser.acquire();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    detector_temp.getA_image_bitmap().recycle();
                                    g_users_imageview.remove(detector_temp);
                                }
                            }
                            g_users_imageview = null;
                        }
                        g_managerInteraction.getSem().release();
                    }
                }
            };

            timeleader.scheduleWithFixedDelay(display_multi_user, 20, 15, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d("DEBUG THREAD", "THREAD NOT RUNNING");
        }


    }

    // Verifie si Internet est disponible (Mobile ou Wifi)
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public void downloadRoom(String idRoom) {
        // verifie si internet est disponible
        if (isNetworkAvailable()) {
            // tentative de cnonection verif?
            //ServerConnection v_connection = new ServerConnection("Guillaume", "toto");

            ServerConnection v_connection = new ServerConnection(User_Name, "toto");
            v_connection.start();
            try {
                v_connection.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            user_id = v_connection.GetUserID();

            // download a room with id
            ServerDownloadRoom v_roomgetter = new ServerDownloadRoom(idRoom, getCacheDir().getAbsolutePath() + "/salles");
            v_roomgetter.start();

            try {
                v_roomgetter.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            current_room = v_roomgetter.getA_cachedir() + "/" + v_roomgetter.getA_room_name();

            File v_dircache = new File(current_room + "/Sensibilities");
            File[] v_sens_files = v_dircache.listFiles();
            try {
                listColorsDownloaded = objectParser.parseColors(v_sens_files[0].getAbsolutePath());
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                Log.d("PARSE", "Parsing : " + e);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("PARSE", "IO : " + e);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Log.e("PARSE", "NullPointer : " + e);
            }
        } else {
            File v_dircache = new File(current_room + "/Sensibilities");
            File[] v_sens_files = v_dircache.listFiles();
            ArrayList<TactosColor> listecache = null;

            error_Show(ERROR_INTERNET);
        }
    }

    // Mode Compatibilité PC : Calcul Ratio + Zone d'affichage contenu PC
    public void display_PC() {
        ratio_x = (float) (width / 1024.0);
        ratio_y = ratio_x;
        System.out.println(ratio_y);
        if (width > height) {
            Zone_PC = new Rect(25, 0, (int) (width * ratio_x), height);
            ratio_x = (float) (Zone_PC.height() / 768.0);
            ratio_y = (float) (Zone_PC.height() / 768.0);
        } else {
            Zone_PC = new Rect(25, height / 4, width - 25, (int) (height * ratio_y));
            ratio_x = (float) (Zone_PC.width() / 1024.0);
            ratio_y = (float) (Zone_PC.width() / 1024.0);
        }


    }

    // Conversion script Lua passé en paramètre vers une version lisible pour mobile
    private void convertLua(String fichier) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(current_room + "/Scripts/" + fichier)), "ISO-8859-1")); // permet de lire le script lua choisit
            String ligne;
            File file = new File(current_room + "/Scripts/" + fichier.split("\\.")[0] + "_mobile.lua");
            file.createNewFile();
            FileOutputStream fileStream = new FileOutputStream(file);
            OutputStreamWriter bw = new OutputStreamWriter(fileStream, Charset.forName("UTF-8")); // creation d'un fichier qui permettra d'utiliser le script lua

            bw.write("tactos = luajava.bindClass( \"com.bluetooth.dev.lua_test.TactosCore\" )" + "\n"); // ajout de tactos / tactos est une variable de type TactosCore ( permettra d'utiliser les fonctions dans la classe TactosCore )
            bw.write("tactosM = luajava.bindClass( \"com.bluetooth.dev.lua_test.MainActivity\" )" + "\n");// ajout de tactosM / tactosM est une variable de type MainActivity( meme idee que tactos ) // a supprimer
            int nbLigne = 1;
            while ((ligne = in.readLine()) != null) {
                //TactosCore.Debug(ligne);
                Log.e("ligne " + nbLigne, " " + ligne);
                nbLigne++;
                for (int i = 0; i < RegisterLua.tabFonction.length; i++) {
                    if (ligne.contains(RegisterLua.tabFonction[i]) && !ligne.contains("require")) {
                        ligne = ligne.replaceAll(RegisterLua.tabFonction[i], RegisterLua.CompareString(RegisterLua.tabFonction[i]));
                    } // remplace certaine fonction lua pour pouvoir utiliser ceux du java a partir d'un register ( RegisterLua )
                }
                bw.write(ligne + "\n"); // ecriture de la ligne du script lua dans le nouveau fichier

            }
            bw.flush();
            bw.close(); // fermeture des ecrits

            BufferedReader testdelec = new BufferedReader(new InputStreamReader(new FileInputStream(new File(current_room + "/Scripts/" + fichier.split("\\.")[0] + "_mobile.lua")), "ISO-8859-1"));
            while ((ligne = testdelec.readLine()) != null) {
                Log.e("l ", " " + ligne);
            }
            //fw.flush();
            //fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //toucher l'activité / l'écran
        // Rentransmet le MotionEvent au detecteur de Geste
        positionD.set((int) event.getX(), (int) event.getY()); //position du doigt sur l'ecran
        this.gDetect.onTouchEvent(event);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        DetecteurPixel.testPixel(); // fonction pour detecter un certain type de pixel

        boolean flag_notdetected = true;
        boolean objetDetect = false;
        FormDetector v_detecteur_temp;
        TactosImage img_tmp;
        TactosSound sound_tmp;
        TactosText txt_tmp;
        TactosDoor door_tmp;
        TactosIcon icon_tmp;

        // Création d'un Iterator sur le LinkedList
        Iterator itr;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // doigt posé sans mouvement

                // Parcours toutes les listes chainée pour verifier si on est sur un element
                // et renvoie l'evenement en conséquence

                // Detection Image

                // commentaire explicative au niveau de action move, action down et action move equivalent

                for (int i = 0; i < listeObjet.size(); i++) {
                    if(listeObjet.get(i) == null) {
                        Log.e("objet " + i, " est null");
                        listeObjet.remove(i);
                    }
                }

                for (int i_lis = 0; i_lis < listeObjet.size(); i_lis++) { // on cherche dans la liste d'objet event
                    if (listeObjet.get(i_lis).getX() <= event.getX() && listeObjet.get(i_lis).getX_P_Width() >= event.getX()
                            && listeObjet.get(i_lis).getY() <= event.getY() && listeObjet.get(i_lis).getY_P_Height() >= event.getY()) { // test si le doitg est sur un objet
                        boolean verif_mouseEnter = true;
                        for (ObjetEvent liObImg : flagMouseEnter) {// test si l'objet a deja fait son mouseEnter ou qu'il possede cette evenement
                            if (liObImg == listeObjet.get(i_lis) && !listeObjet.get(i_lis).getEvenement().equals("mouseEnter")) {
                                verif_mouseEnter = false;
                            }
                        }
                        if (verif_mouseEnter && listeObjet.get(i_lis).getEvenement().equals("mouseEnter") && listeObjet.get(i_lis).isFlag_evenement()) { // si oui on execute l'evenement et on l'ajoute a ceux qui l'ont deja fait
                            listeObjet.get(i_lis).setFlag_evenement(false);
                            flagMouseEnter.add(listeObjet.get(i_lis));
                            onEventLua(listeObjet.get(i_lis));
                        }

                        if (event.getPointerCount() == 1 && listeObjet.get(i_lis).getEvenement().equals("mouseLeftUp") && listeObjet.get(i_lis).isFlag_evenement()) { // test si l'objet possede un evenement click
                            listeObjet.get(i_lis).setFlag_evenement(false);
                            flagClick.add(listeObjet.get(i_lis));
                        }
                        if (listeObjet.get(i_lis).getEvenement().equals("mouseExit") && listeObjet.get(i_lis).isFlag_evenement()) { // test si l'objet possede un mouseExit
                            listeObjet.get(i_lis).setFlag_evenement(false);
                            flagExit.add(listeObjet.get(i_lis));
                        }

                        flag_notdetected = false; // si au moins un objet est trouve, on l'indique

                    }
                }

                for (ObjetEvent liobim : flagExit) {
                    if (liobim != null && !(liobim.getX() <= event.getX() && liobim.getX_P_Width() >= event.getX()
                            && liobim.getY() <= event.getY() && liobim.getY_P_Height() >= event.getY())) {
                        liobim.setFlag_evenement(true);
                        onEventLua(liobim);
                        flagExit.set(flagExit.indexOf(liobim), null);
                    } // si l'objet est en dehors des limites on le supprime et on execute le mouseExit
                }
                for (ObjetEvent liobim : flagMouseEnter) {
                    if (liobim != null && !(liobim.getX() <= event.getX() && liobim.getX_P_Width() >= event.getX()
                            && liobim.getY() <= event.getY() && liobim.getY_P_Height() >= event.getY())) {
                        liobim.setFlag_evenement(true);
                        flagMouseEnter.set(flagMouseEnter.indexOf(liobim), null);
                    }// si l'objet est en dehors des limites
                }
                for (ObjetEvent liobim : flagClick) {
                    if (liobim != null && !(liobim.getX() <= event.getX() && liobim.getX_P_Width() >= event.getX()
                            && liobim.getY() <= event.getY() && liobim.getY_P_Height() >= event.getY())) {
                        liobim.setFlag_evenement(true);
                        flagClick.set(flagClick.indexOf(liobim), null);
                    }// si l'objet est en dehors des limites
                }

                if (sharedPrefs.getBoolean("tacticonActive", true)) {
                    itr = IconList.iterator();
                    while (itr.hasNext()) {
                        icon_tmp = (TactosIcon) itr.next();
                        if (icon_tmp.r.contains((int) event.getX(), (int) event.getY())) {
                            if (!icon_tmp.in) {
                                icon_tmp.onEvent(TactosObject.event_mouseEnter);
                                icon_tmp.in = true;
                            }
                        }
                    }
                }

                // s'il y a interaction multi utiisateurs
                if (g_managerInteraction != null && g_managerInteraction.isRunning()) {
                    // envoie les positions courantes de l'utilisateurs
                    sendUsersPosition(g_managerInteraction, (int) ((event.getX() - Zone_PC.left - 4)), (int) ((event.getY() - Zone_PC.top - 4)));

                    // si la liste d'utilisateurs affichés à l'écran n'est pas vide
                    if (g_users_imageview != null) {
                        // parcours de la liste
                        for (Map.Entry<Integer, FormDetector> entry : g_users_imageview.entrySet()) {
                            FormDetector detector_temp = entry.getValue();
                            if (detector_temp.getA_image_view() != null) // si l'image est bien définie
                            {
                                // si le doigt touche l'image courante
                                if (detector_temp.isPixelInView(detector_temp.getA_image_view(), (int) event.getRawX(), (int) event.getRawY())) // Si on touche à l'image en partant de l'écran, alors je detecte
                                {
                                    // mise a l'échelle et soustraction du decalage de l'image a l'écran
                                    float xi = (event.getX() - detector_temp.getA_decalage_x()) * detector_temp.getA_WidthRatio(); // coord X de l'ImageView * le ratio X
                                    float yi = (event.getY() - detector_temp.getA_decalage_y()) * detector_temp.getA_HeightRatio(); // coord Y de l'ImageView * le ratio Y

                                    // envoi de la position du doigt par rapport à l'image, detection s'il touche ou non une couleur reconnaissable
                                    detector_temp.testDetection(xi, yi);
                                    flag_notdetected = false;
                                }
                            }
                        }
                    }

                }

                if (flag_notdetected) {
                    // sinon je suis sur l'écran de base, je ne detecte pas
                    Log.d("DEBUG_DETECTION", "Ecran");
                    //Log.e("position du doigt", " X = " + event.getX() + ", Y = " + event.getY());
                    //flag_mouseEnter = true;
                    //flag_click = false;
                    if (flagMouseEnter.size() != 0)
                        flagMouseEnter.clear();
                    if (flagClick.size() != 0)
                        flagClick.clear();
                    if (flagExit.size() != 0)
                        flagExit.clear();
                    for (int i = 0; i < flagMouseEnter.size(); i++) {
                        flagMouseEnter.remove(i);
                    }
                    for (int i = 0; i < flagClick.size(); i++) {
                        flagClick.remove(i);
                    }
                    for (int i = 0; i < flagExit.size(); i++) {
                        flagExit.remove(i);
                    } // on vide les listes des evenements

                }
                break;


            case MotionEvent.ACTION_MOVE: // doigt posé en mouvement
                // on vérifie chaque liste chainée


                Log.e("x et y ", " "+event.getX()+" / "+event.getY());
                for (int i = 0; i < listeObjet.size(); i++) {
                    if(listeObjet.get(i) == null) {
                        Log.d("objet " + i, " est null");
                        listeObjet.remove(i);
                    }
                } // si un objet est null -> suppression

                for (int i_lis = 0; i_lis < listeObjet.size(); i_lis++) { // on cherche dans la liste d'objet event
                    if (listeObjet.get(i_lis).getX() <= event.getX() && listeObjet.get(i_lis).getX_P_Width() >= event.getX()
                            && listeObjet.get(i_lis).getY() <= event.getY() && listeObjet.get(i_lis).getY_P_Height() >= event.getY()) { // test si le doitg est sur un objet
                        boolean verif_mouseEnter = true;
                        for (ObjetEvent liObImg : flagMouseEnter) {// test si l'objet a deja fait son mouseEnter ou qu'il possede cette evenement
                            if (liObImg == listeObjet.get(i_lis) && !listeObjet.get(i_lis).getEvenement().equals("mouseEnter")) {
                                verif_mouseEnter = false;
                            }
                        }
                        if (verif_mouseEnter && listeObjet.get(i_lis).getEvenement().equals("mouseEnter") && listeObjet.get(i_lis).isFlag_evenement()) { // si vrai on execute l'evenement et on l'ajoute a ceux qui l'ont deja fait
                            listeObjet.get(i_lis).setFlag_evenement(false);
                            flagMouseEnter.add(listeObjet.get(i_lis));
                            onEventLua(listeObjet.get(i_lis));
                        }

                        if (event.getPointerCount() == 1 && listeObjet.get(i_lis).getEvenement().equals("mouseLeftUp") && listeObjet.get(i_lis).isFlag_evenement()) { // test si l'objet possede un evenement click
                            listeObjet.get(i_lis).setFlag_evenement(false);
                            flagClick.add(listeObjet.get(i_lis)); // ajout a la liste des click
                        }
                        if (listeObjet.get(i_lis).getEvenement().equals("mouseExit") && listeObjet.get(i_lis).isFlag_evenement()) { // test si l'objet possede un mouseExit
                            listeObjet.get(i_lis).setFlag_evenement(false);
                            flagExit.add(listeObjet.get(i_lis)); // ajout a la liste des exit
                        }

                        flag_notdetected = false; // si au moins un objet est trouve, on l'indique

                    }
                }

                for (ObjetEvent liobim : flagExit) {
                    if (liobim != null && !(liobim.getX() <= event.getX() && liobim.getX_P_Width() >= event.getX()
                            && liobim.getY() <= event.getY() && liobim.getY_P_Height() >= event.getY())) {
                        liobim.setFlag_evenement(true);
                        onEventLua(liobim);
                        flagExit.set(flagExit.indexOf(liobim), null);
                    } // si l'objet est en dehors des limites on le supprime et on execute le mouseExit
                }
                for (ObjetEvent liobim : flagMouseEnter) {
                    if (liobim != null && !(liobim.getX() <= event.getX() && liobim.getX_P_Width() >= event.getX()
                            && liobim.getY() <= event.getY() && liobim.getY_P_Height() >= event.getY())) {
                        liobim.setFlag_evenement(true);
                        flagMouseEnter.set(flagMouseEnter.indexOf(liobim), null);
                    }// si l'objet est en dehors des limites
                }
                for (ObjetEvent liobim : flagClick) {
                    if (liobim != null && !(liobim.getX() <= event.getX() && liobim.getX_P_Width() >= event.getX()
                            && liobim.getY() <= event.getY() && liobim.getY_P_Height() >= event.getY())) {
                        liobim.setFlag_evenement(true);
                        flagClick.set(flagClick.indexOf(liobim), null);
                    }// si l'objet est en dehors des limites
                }

                if (sharedPrefs.getBoolean("tacticonActive", true)) {
                    itr = IconList.iterator();
                    while (itr.hasNext()) {
                        icon_tmp = (TactosIcon) itr.next();
                        if (icon_tmp.r.contains((int) event.getX(), (int) event.getY())) {
                            if (!icon_tmp.in) {
                                icon_tmp.onEvent(TactosObject.event_mouseEnter);
                                icon_tmp.in = true;
                            }
                        }
                    }
                }

                // s'il y a interaction multi utiisateurs
                if (g_managerInteraction != null && g_managerInteraction.isRunning()) {
                    // envoie les positions courantes de l'utilisateurs
                    sendUsersPosition(g_managerInteraction, (int) ((event.getX() - Zone_PC.left - 4)), (int) ((event.getY() - Zone_PC.top - 4)));

                    // si la liste d'utilisateurs affichés à l'écran n'est pas vide
                    if (g_users_imageview != null) {
                        // parcours de la liste
                        for (Map.Entry<Integer, FormDetector> entry : g_users_imageview.entrySet()) {
                            FormDetector detector_temp = entry.getValue();
                            if (detector_temp.getA_image_view() != null) // si l'image est bien définie
                            {
                                // si le doigt touche l'image courante
                                if (detector_temp.isPixelInView(detector_temp.getA_image_view(), (int) event.getRawX(), (int) event.getRawY())) // Si on touche à l'image en partant de l'écran, alors je detecte
                                {
                                    // mise a l'échelle et soustraction du decalage de l'image a l'écran
                                    float xi = (event.getX() - detector_temp.getA_decalage_x()) * detector_temp.getA_WidthRatio(); // coord X de l'ImageView * le ratio X
                                    float yi = (event.getY() - detector_temp.getA_decalage_y()) * detector_temp.getA_HeightRatio(); // coord Y de l'ImageView * le ratio Y

                                    // envoi de la position du doigt par rapport à l'image, detection s'il touche ou non une couleur reconnaissable
                                    detector_temp.testDetection(xi, yi);
                                    flag_notdetected = false;
                                }
                            }
                        }
                    }

                }

                if (flag_notdetected) {
                    // sinon je suis sur l'écran de base, je ne detecte pas
                    Log.d("DEBUG_DETECTION", "Ecran");
                    //Log.e("position du doigt", " X = " + event.getX() + ", Y = " + event.getY());
                    //flag_mouseEnter = true;
                    //flag_click = false;
                    if (flagMouseEnter.size() != 0)
                        flagMouseEnter.clear();
                    if (flagClick.size() != 0)
                        flagClick.clear();
                    if (flagExit.size() != 0)
                        flagExit.clear();
                    for (int i = 0; i < flagMouseEnter.size(); i++) {
                        flagMouseEnter.remove(i);
                    }
                    for (int i = 0; i < flagClick.size(); i++) {
                        flagClick.remove(i);
                    }
                    for (int i = 0; i < flagExit.size(); i++) {
                        flagExit.remove(i);
                    } // on vide les listes des evenements

                }
                break;
            case MotionEvent.ACTION_UP: // doigt levé
                for (ObjetEvent liobim : flagMouseEnter) {
                    if (liobim != null)
                        liobim.setFlag_evenement(true);
                }
                if (flagMouseEnter.size() != 0) {
                    flagMouseEnter.clear();
                }
                if (flagExit.size() != 0) {
                    flagExit.clear();
                }
                for (int i = 0; i < flagMouseEnter.size(); i++) {
                    flagMouseEnter.remove(i);
                }
                for (int i = 0; i < flagClick.size(); i++) {
                    if (flagClick.get(i) != null) {
                        flagClick.get(i).setFlag_evenement(true);
                        flagClick.set(i, null);
                    }
                    flagClick.remove(i);
                }
                for (int i = 0; i < flagExit.size(); i++) {
                    flagExit.remove(i);
                } // on vide les listes flags
                TactosAction.setA_finalMatrice(new boolean[16]);
                tacicon_running = false;
                allObjectOut();
                objetDetect = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: //multi-doigt
                pointeur_count = event.getPointerCount();
                for (int i_lis = 0; i_lis < listeObjet.size(); i_lis++)
                    for (ObjetEvent liobimg : flagClick) {
                        if (pointeur_count == 2 && liobimg == listeObjet.get(i_lis)) {
                            onEventLua(liobimg);
                            //listeObjet.get(i_lis).setFlag_evenement(true);
                            //flagClick.set(flagClick.indexOf(liobimg), null);
                        }
                    }
                /*if(objetDetect){
                    onEventLua(2);
                }*/
                Log.e("DEBUG_POINTER", "NOMBRE DE DOIGTS : " + pointeur_count);
                break;
        }
        // On envoie les données au boitier
        /*if (TactosAction.getA_finalMatrice() != null && !tacicon_running) { // a garder pour savoir comment changer
            result = TactosAction.getA_finalMatrice();
            result = ArrayConvert.simpleToRegular(result);
            result = ArrayConvert.mirrorX16(result);
            try {
                co.sendData(ArrayConvert.regularBoolToByte(ArrayConvert.split16to8(result, 1)),
                        ArrayConvert.regularBoolToByte(ArrayConvert.split16to8(result, 2)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }*/

        return super.onTouchEvent(event);
    }

    // envoie les donnees de position au thread d'interaction
    public void sendUsersPosition(ServerInteraction interaction_manager, int x, int y) {
        if (interaction_manager != null && interaction_manager.isRunning()) {
            interaction_manager.setPosx(x);
            interaction_manager.setPosy(y);
        }

    }


    ///////////////////// AJOUT D'ELEMENTS POUR LES DIFFERNTES LISTES CHAINES TACTOSOBJECT/////////

    // Utiliser pour gerer l'event onMouseExit (in mis a false)
    public void allObjectOut() {
        for (TactosImage img : ImgList) {
            img.in = false;
        }
        for (TactosSound sound : SoundList) {
            sound.in = false;
        }
        for (TactosText txt : TextList) {
            txt.in = false;
        }
        for (TactosDoor door : DoorList) {
            door.in = false;
        }
        for (TactosIcon icon : IconList) {
            icon.in = false;
        }
        tacicon_running = false;

    }

    // onEvent a modifier pour detecter si présence de script Lua et présence d'une fonction
    // onEvent. Sinon appel fonction nil et crash
    public void onEventLua(ObjetEvent ob) {
        // dom uncomment :
        //globals.get("onEvent").call(LuaValue.valueOf(id));
        if (ob.identification().equals("DOOR") || ob.identification().equals("SOUND")) { // si l'objet est une porte ou un son / fonction active appeler
            if (ob.identification().equals("DOOR")) { // si l'objet est une porte, on active la fonction onAction du script si elle existe
                try {
                    globals.get("onAction").call(LuaValue.valueOf(ob.getID()));
                } catch (LuaError err) {
                    Log.d("onAction", " n'est pas appeler ici");
                }
            }
            ob.active();
        } else
            runOnEvent(ob); // sinon : new thread pour utiliser onEvent du script Lua
    }

    // Fonction qui efface tout le contenu a l'écran
    // Non implémenté pour l'instant
    public void ClearContent() {
        for (TactosImage img : ImgList) {
            img.delete();
        }
        for (TactosText txt : TextList) {
            txt.delete();
        }

        ImgList.clear();
        SoundList.clear();
        TextList.clear();
        DoorList.clear();
        IconList.clear();
    }

    public void addImageToList(ImageView img, String img_path) {
        ImgList.add(new TactosImage(img, img_path));

        // voir setImageResource(int)
        //getMainLayout().addView(img);
    }

    public void addDoorToList(int ID, int x, int y, int w, int h, String data) {
        DoorList.add(new TactosDoor(ID, x, y, w, h, data));
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public void addTextTolist(TextView txt, String event, String data) {
        TextList.add(new TactosText(txt, event, data));
    }

    public void addSoundToList(int ID, int x, int y, int w, int h, String event, String data) {
        SoundList.add(new TactosSound(ID, x, y, w, h, event, data));
    }

    public void addTacticonToList(int ID, int x, int y, int w, int h, String event, String data) {
        IconList.add(new TactosIcon(ID, x, y, w, h, event, data));
    }


    /////////////////////// GETTER ///////////////////////////////////

    // /!\ Voir pour remplacer Thread.sleep()
    public void exec_tacticon(final String FilePath, final int ID) {
        new Thread() {
            @Override
            public void run() {
                boolean repeat = false;
                boolean loaded = false;
                boolean[] tmp = new boolean[16];
                tacicon_running = true;
                ArrayList<Integer> tempo = new ArrayList<Integer>();
                ArrayList<boolean[]> tacticon = new ArrayList<boolean[]>();
                tacticon_current_id = ID;
                while (tacicon_running && ID == tacticon_current_id) {
                    if (!loaded) {
                        try {
                            File f = new File(FilePath);
                            FileReader fr = new FileReader(f);
                            BufferedReader br = new BufferedReader(fr);
                            try {
                                String line = br.readLine();
                                int tab_count = 0;
                                while (line != null) {
                                    if (line.contains("TPS="))
                                        tempo.add(Integer.parseInt(line.split("=")[1]));
                                    else if (line.contains("REPEAT"))
                                        repeat = true;
                                    else if (tab_count < 4) {
                                        for (int i = 0; i < line.length(); i++) {
                                            if (line.charAt(i) == '0') {
                                                tmp[i + 4 * tab_count] = false;
                                            } else if (line.charAt(i) == '1') {
                                                tmp[i + 4 * tab_count] = true;
                                            }
                                        }
                                        tab_count++;
                                    }
                                    if (tab_count == 4) {
                                        tacticon.add(tmp);
                                        tab_count = 0;
                                        tmp = new boolean[16];
                                    }
                                    System.out.println(line);
                                    line = br.readLine();
                                }
                                br.close();
                                fr.close();
                                loaded = true;
                            } catch (IOException exception) {
                                System.out.println("Erreur lors de la lecture : " + exception.getMessage());
                            }
                        } catch (FileNotFoundException exception) {
                            System.out.println("Le fichier n'a pas été trouvé");
                        }
                    }
                    if (repeat) {
                        tacticon_current = new boolean[16];
                        for (int i = 0; i < tacticon.size(); i++) {

                            //if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            tacticon_current = tacticon.get(i);
                            tacticon_current = ArrayConvert.simpleToRegular(tacticon_current);
                            tacticon_current = ArrayConvert.mirrorX16(tacticon_current);
                            try {
                                co.sendData(ArrayConvert.regularBoolToByte(ArrayConvert.split16to8(tacticon_current, 1)),
                                        ArrayConvert.regularBoolToByte(ArrayConvert.split16to8(tacticon_current, 2)));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            try {
                                Thread.currentThread().sleep(tempo.get(i));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //}
                            //else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            /*    tacticon_current = tacticon.get(i);
                                tacticon_current = ArrayConvert.simpleToRegular(tacticon_current);
                                tacticon_current = ArrayConvert.mirrorX16(tacticon_current);
                                tacticon_current = ArrayConvert.rotationR16(tacticon_current);
                                co.sendData(ArrayConvert.regularBoolToByte(ArrayConvert.split16to8(tacticon_current, 1)),
                                        ArrayConvert.regularBoolToByte(ArrayConvert.split16to8(tacticon_current, 2)));
                                try {
                                    Thread.currentThread().sleep(tempo.get(i));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                            */   // }
                            //}
                        }
                    }

                }

            }
        }.start();
    }

    // Fonction retournant message d'erreur a l'utilisateur
    // Voir constantes ERROR a passer en paramètres suivant l'erreur
    private void error_Show(int ID_ERROR) {
        if (ID_ERROR == ERROR_BLUETOOTH) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder
                    .setTitle("Erreur")
                    .setMessage("Impossible de se connecter au Module Bluetooth")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, close
                            // current activity
                            // Dom -                          MainActivity.getInstance().finish();
                        }
                    });
            AlertDialog errorDialog = alertDialogBuilder.create();
            errorDialog.show();
            TactosCore.Say("Erreur de la connexion Bluetooth");
        } else if (ID_ERROR == ERROR_INTERNET) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder
                    .setTitle("Erreur")
                    .setMessage("Impossible de se connecter a Internet")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, close
                            // current activity
                            MainActivity.getInstance().finish();
                        }
                    });
            AlertDialog errorDialog = alertDialogBuilder.create();
            errorDialog.show();
            TactosCore.Say("Erreur de la connexion Internet");
        }
    }

    // Fonction de bases non utilisés (utilisé par la barre d'action en haut que l'on a supprimé)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Classe de gestion des gestes
    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        //class content
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                    }
                    result = true;
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                }
                result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }

        // Differents gestes de balayements
        public void onSwipeRight() {
            if (pointeur_count == 2) {
                System.out.println("SWIPE RIGHT");
                pointeur_count = 0;
            }
        }

        public void onSwipeLeft() {
            if (pointeur_count == 2) {
                System.out.println("SWIPE LEFT");
                pointeur_count = 0;
            }
        }

        public void onSwipeTop() {
            if (pointeur_count == 2) {
                System.out.println("SWIPE TOP");
                pointeur_count = 0;
            }
            if (pointeur_count == 3) { // si on glisse 3 doigts vers le haut -> ouverture des options
                pointeur_count = 0;
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        }

        public void onSwipeBottom() {
            if (pointeur_count == 2) {
                System.out.println("SWIPE BOTTOM");
                pointeur_count = 0;
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d("TEST DOUBLE TAP", "DOUBLE TAP DETECTED");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            System.out.println("LONG PRESS");
        }
    }

    public static void Redraw() {
        ArrayList<Bitmap> fusionDessein = new ArrayList<Bitmap>();
        ArrayList<Integer> position = new ArrayList<Integer>();
        for (int i = 0; i < MainActivity.getInstance().listeObjet.size(); i++) {
            if (MainActivity.getInstance().listeObjet.get(i).identification().equals("IMAGE")) { // si l'objet est une image
                fusionDessein.add(Bitmap.createScaledBitmap(((BitmapDrawable) ((ImageView) MainActivity.getInstance().listeObjet.get(i).tactosType()).getDrawable()).getBitmap(),
                        MainActivity.getInstance().listeObjet.get(i).getX_P_Width() - MainActivity.getInstance().listeObjet.get(i).getX(), MainActivity.getInstance().listeObjet.get(i).getY_P_Height() - MainActivity.getInstance().listeObjet.get(i).getY(), false));
                position.add(i); // alors on la convertit en bitmap et on sauvegarde sa position
            }
            if (MainActivity.getInstance().listeObjet.get(i).identification().equals("TEXT")) { // si l'objet est du text
                Log.e("X et Y ", " " + MainActivity.getInstance().listeObjet.get(i).getX() + " , " + MainActivity.getInstance().listeObjet.get(i).getY());
                Log.e("taille image", " " + ((ObjetText) MainActivity.getInstance().listeObjet.get(i)).getTextImg().getX() + " , " + ((ObjetText) MainActivity.getInstance().listeObjet.get(i)).getTextImg().getY());
                //Log.e("width et height ", " "+listeObjet.get(i).getWidth()+" , "+listeObjet.get(i).getHeight());
                //Log.e("taille image", " "+((ObjetText)listeObjet.get(i)).getTextImg().getWidth()+" , "+((ObjetText)listeObjet.get(i)).getTextImg().getHeight());
                fusionDessein.add(Bitmap.createScaledBitmap(((BitmapDrawable) (((ObjetText) MainActivity.getInstance().listeObjet.get(i)).getTextImg()).getDrawable()).getBitmap(),
                        MainActivity.getInstance().listeObjet.get(i).getX_P_Width() - MainActivity.getInstance().listeObjet.get(i).getX(), MainActivity.getInstance().listeObjet.get(i).getY_P_Height() - MainActivity.getInstance().listeObjet.get(i).getY(), false));
                position.add(i); // idem
            }
        }
        Display disMain = MainActivity.getInstance().getWindowManager().getDefaultDisplay();
        Point pMain = new Point();
        disMain.getSize(pMain); // recuperation des dimension de l'appareil
        Bitmap principaleImg = Bitmap.createBitmap(pMain.x, pMain.y, Bitmap.Config.ARGB_8888);
        Canvas can = new Canvas(principaleImg); // definition de l'image principal
        Log.e("s redraw", "ok");

        Bitmap fond = BitmapFactory.decodeResource(MainActivity.getContext().getResources(), R.drawable.fond_blanc);
        Bitmap fond2 = Bitmap.createScaledBitmap(fond, pMain.x, pMain.y, false); // definition d'un fond blanc
        can.drawBitmap(fond2, 0, 0, null); // ajout du fond bland dans principal
        Log.e("size listeOb ", " " + MainActivity.getInstance().listeObjet.size());
        Log.e("size fusionDe ", " " + fusionDessein.size());
        Log.e("size position ", " " + position.size());
        for (int i = 0; i < position.size(); i++) {
            can.drawBitmap(fusionDessein.get(i), MainActivity.getInstance().listeObjet.get(position.get(i)).getX(), MainActivity.getInstance().listeObjet.get(position.get(i)).getY(), null);
        } // pour chaque image de fusionDessein, on la colle sur l'image principal
        //MainActivity.getMainLayout().removeView(finalImg); // on efface l'image de fond de l'appareil
        MainActivity.getInstance().runRedraw(finalImg, principaleImg);
        //finalImg.setImageBitmap(principaleImg); // on lui ajoute principal
        //MainActivity.getMainLayout().addView(finalImg); //on la reintegre
        /*position.clear();
        fusionDessein.clear();
        for (int i = 0; i < position.size(); i++) {
            position.remove(i);
            fusionDessein.remove(i);
        }*/ // on vide les tableaux utiliser
        //MainActivity.getMainLayout().removeView(testText);
        //MainActivity.getMainLayout().addView(testText); // on redessine les picos pour les tests
    }

    public static void Draw(ImageView image, int x, int y, int width, int height) {
        try {
            if (image != null) {
                /*if (testText != null) {
                    MainActivity.getMainLayout().removeView(testText);
                }*/
                //MainActivity.getMainLayout().removeView(finalImg); // on retire l'image du fond de l'appareil
                Display disMain = MainActivity.getInstance().getWindowManager().getDefaultDisplay();
                Point pMain = new Point();
                disMain.getSize(pMain); // recuperation de la taille de l'appareil
                Bitmap principaleImg = Bitmap.createScaledBitmap(((BitmapDrawable) finalImg.getDrawable()).getBitmap(), pMain.x, pMain.y, false); // conversion de l'image en bitmap
                Canvas can = new Canvas(principaleImg); // rend possible le dessein pour le bitmap

                Bitmap imgTemp = Bitmap.createScaledBitmap(((BitmapDrawable) image.getDrawable()).getBitmap(), width, height, false); // recuperation de l'image en parametre et conversion en bitmap
                can.drawBitmap(imgTemp, x, y, null); // on colle ce bitmap precedent sur l'image de fond
                //finalImg.setImageBitmap(principaleImg);
                //MainActivity.getMainLayout().addView(finalImg); // reintegre l'image de fond sur l'appareil
                MainActivity.getInstance().runRedraw(finalImg, principaleImg);
                /*if (testText != null) {
                    MainActivity.getMainLayout().addView(testText);
                }*/
        /*for (ObjetEvent ob : listeObjet) {
            if(ob.identification().equals("TEXT")) {
                MainActivity.getMainLayout().removeView((TextView) ob.tactosType());
                MainActivity.getMainLayout().addView((TextView)ob.tactosType());
            }
        }*/
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            MainActivity.getInstance().imageDefectueuse.add(image);
            Log.e("pb", " fonction draw");
        }
    }

    public static void DeleteObject(String type, int Id) {
        boolean verif;
        for (int i = 0; i < MainActivity.getInstance().listeObjet.size(); i++) {
            if (MainActivity.getInstance().listeObjet.get(i).getID() == Id) { // on cherche l'objet a partir de l'id
                verif = false;
                if (type.equals("IMAGE"))
                    verif = true;
                if (type.equals("TEXT"))
                    verif = true;
                Log.e("liste size ", " " + MainActivity.getInstance().listeObjet.size());
                MainActivity.getInstance().listeObjet.remove(i); // si trouver on la supprime
                Log.e("liste size ", " "+MainActivity.getInstance().listeObjet.size());
                if (verif) // si c'etait du text ou une image
                    MainActivity.getInstance().Redraw(); // on redessine l'image de fond
            }
        }
    }

    public LuaValue getGlobals() {
        return globals;
    } // sert a retouner le script lua


}





