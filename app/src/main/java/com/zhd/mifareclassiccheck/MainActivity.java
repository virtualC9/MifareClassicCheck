package com.zhd.mifareclassiccheck;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /**
     * Placeholder for not found keys.
     */
    public static final String NO_KEY = "------------";
    /**
     * Placeholder for unreadable blocks.
     */
    public static final String NO_DATA = "--------------------------------";
    private ArrayList<byte[]> mKeysWithOrder;
    private MifareClassic mMFC = null;
    private int mFirstSector;
    private int mLastSector;
    private int mKeyMapStatus = 0;
    private int mProgressStatus;
    private SparseArray<byte[][]> mKeyMap = new SparseArray<>();
    private StringBuilder stringBuilder;
    private TextView textView;
    private Button button;
    private PendingIntent mPendingIntent;
    private NfcAdapter defaultAdapter;
    boolean isDone = true;
    private Tag mTag;
    private String resultHexStr = "";
    String keyA1 = "A0A1A2A3A4A5";
    String keyA2 = "D3F7D3F7D3F7";
    String keyB = "FFFFFFFFFFFF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        copyFilesFromAssets(this, "std.keys", Environment.getExternalStorageDirectory().getPath(), "std.keys", this);
//        copyFilesFromAssets(this, "extended-std.keys", Environment.getExternalStorageDirectory().getPath(), "extended-std.keys", this);
        // 获得Adapter对象
        defaultAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        textView = findViewById(R.id.content);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        button = findViewById(R.id.button);
        findViewById(R.id.write).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    writeTag();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    textView.setText("写入失败");
                }
            }
        });
        findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateKeyMap();
            }
        });
    }

    /**
     * 写标签
     */
    private void writeTag() throws InterruptedException {
        if (mTag != null) {
//            ArrayList<File> keyFiles = new ArrayList<>();
//            keyFiles.add(new File(Environment.getExternalStorageDirectory().getPath(), "extended-std.keys"));//"/MifareClassicTool/key-files"
//            //读取密匙库
//            if (keyFiles.size() > 0) {
//                // Set key files.
//                File[] keys = keyFiles.toArray(new File[keyFiles.size()]);
//                if (!setKeyFile(keys, MainActivity.this)) {
//                    // Error.
//                    close();
//                    return;
//                }
//            }
            if (!mMFC.isConnected()) {
                connect();
                Thread.sleep(500);
            }
            mFirstSector = 0;
            mLastSector = mMFC.getSectorCount() - 1;
            mProgressStatus = -1;
            stringBuilder = new StringBuilder();
//            while (mProgressStatus < mLastSector) {
//                mProgressStatus = buildNextKeyMapPart();
//                if (mProgressStatus == -1) {
//                    stringBuilder.append("映射失败\n");
//                    break;
//                }
//            }

            String content = "近场通信（Near Field Communication，简称NFC），是一种新兴的技术，使用了NFC技术的设备（例如移动电话）可以在彼此靠近的情况下进" +
                    "行数据交换，是由非接触式射频识别（RFID）及互连互通技术整合演变而来的，通过在单一芯片上集成感应式读卡器、感应式卡片和点对点通信的功能，" +
                    "利用移动终端实现移动支付、电子票务、门禁、移动身份识别、防伪等应用。NFC的中文全称为近场通信技术。NFC是在非接触式射频识别(RFID)技术的基" +
                    "础上，结合无线互连技术研发而成，它为我们日常生活中越来越普及的各种电子产品提供了一种十分安全快捷的通信方式。NFC中文名称中的“近场”是" +
                    "指临近电磁场的无线电波。 因为无线电波实际上就是电磁波，所以它遵循麦克斯韦方程，电场和磁场在从发射天线传播到接收天线的过程会一直交替进" +
                    "行能量转换，并在进行转换时相互增强，例如我们的手机所使用的无线电信号就是利用这种原理进行传播的，这种方法称作远场通信。而在电磁波10个波长" +
                    "以内，电场和磁场是相互独立的，这时的电场没有多大意义，但磁场却可以用于短距离通讯，我们称之为近场通信。近场通信业务结合了近场通信技" +
                    "术和移动通信技术，实现了电子支付、身份认证、票务、数据交换、防伪、广告等多种功能，是移动通信领域的一种新型业务。近场通信业务增强了移动电" +
                    "话的功能，使用户的消费行为逐步走向电子化，建立了一种新型的用户消费和业务模式。NFC技术的应用在世界范围内受到了广泛关注，国内外的电信运营商" +
                    "、手机厂商等不同角色纷纷开展应用试点，一些国际性协会组织也积极进行标准化制定工作。据业内相关机构预测，基于近场通信技术的手机应用将会成为"
                    ;
            byte[] bytes = content.getBytes();
            int srcPos = 0;
            //循环扇区写入
            for (int sectorIndex = 0; sectorIndex < mLastSector; sectorIndex++) {
                if (srcPos >= bytes.length) {
                    textView.setText("写入完成");
                    Toast.makeText(MainActivity.this, "写入完成", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mMFC.isConnected()) {
                    connect();
                    Thread.sleep(500);
                }
                //验证扇区
                boolean auth = false;
                // Read with key B.
                auth = authenticate(sectorIndex, hexStringToByteArray(keyB), true);
                if (!auth) {
                    auth = authenticate(sectorIndex, hexStringToByteArray(keyA2), false);
                    if (!auth) {
                        auth = authenticate(sectorIndex, hexStringToByteArray(keyA1), false);
                    }
                }
                if (!auth) {
                    //失败重新验证
                    for (int i = 0; i < 3; i++) {
                        auth = authenticate(sectorIndex, hexStringToByteArray(keyB), true);
                        if (!auth) {
                            auth = authenticate(sectorIndex, hexStringToByteArray(keyA2), false);
                            if (!auth) {
                                auth = authenticate(sectorIndex, hexStringToByteArray(keyA1), false);
                                if (auth) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (!auth) {
                        textView.setText("验证失败第" + 2 + "扇区");
                        return;
                    }
                }
                //每次写入必须是16个字节
                byte[] newkeybuf = new byte[16];
                try {
                    System.arraycopy(bytes, srcPos, newkeybuf, 0, 16);
                } catch (Exception e) {
                    //不够16位
                    System.arraycopy(bytes, srcPos, newkeybuf, 0, bytes.length - srcPos - 1);
                }
                if (newkeybuf.length == 0) {
                    break;
                }
                if (sectorIndex < 32) {
                    //每个扇区块循环，每个扇区最后一块为密钥
                    for (int n = 0; n < 3; n++) {
                        if (srcPos >= bytes.length) {
                            textView.setText("写入完成");
                            Toast.makeText(MainActivity.this, "写入完成", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        byte[] newkeybuf2 = new byte[16];
                        try {
                            System.arraycopy(bytes, srcPos, newkeybuf2, 0, 16);
                        } catch (Exception e) {
                            System.arraycopy(bytes, srcPos, newkeybuf2, 0, bytes.length - srcPos - 1);
                        }
                        if (newkeybuf2.length == 0) {
                            break;
                        }
                        //第一区第一块为保留区
                        if (sectorIndex == 0 && n == 0) {
                            continue;
                        }
                        //自动补0
                        if (newkeybuf2.length < 16) {
                            String hexStr = byte2HexString(newkeybuf2);
                            hexStr = String.format("%016s", hexStr);
                            newkeybuf2 = hexStringToByteArray(hexStr);
                        }
                        int blockIndex = mMFC.sectorToBlock(sectorIndex) + n;
                        try {
                            mMFC.writeBlock(blockIndex, newkeybuf2);
                            Thread.sleep(10);
                        } catch (IOException e) {
                            e.printStackTrace();
                            textView.setText("写入失败，第" + sectorIndex + "扇区，第" + n + "块； " + e.getMessage());
                            close();
                            return;
                        }
                        srcPos += 16;
                    }
                } else {
                    for (int n = 0; n < 15; n++) {
                        //TODO 131 132
                        if (srcPos >= bytes.length) {
                            textView.setText("写入完成");
                            Toast.makeText(MainActivity.this, "写入完成", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        byte[] newkeybuf2 = new byte[16];
                        try {
                            System.arraycopy(bytes, srcPos, newkeybuf2, 0, 16);
                        } catch (Exception e) {
                            System.arraycopy(bytes, srcPos, newkeybuf2, 0, bytes.length - srcPos - 1);
                        }
                        if (newkeybuf2.length == 0) {
                            Toast.makeText(MainActivity.this, "写入完成", Toast.LENGTH_SHORT).show();
                            //已写完
                            return;
                        }
                        //自动补0
                        if (newkeybuf2.length < 16) {
                            String hexStr = byte2HexString(newkeybuf2);
                            hexStr = String.format("%016s", hexStr);
                            newkeybuf2 = hexStringToByteArray(hexStr);
                        }
                        int blockIndex = mMFC.sectorToBlock(sectorIndex) + n;
                        try {
                            mMFC.writeBlock(blockIndex, newkeybuf2);
                            Thread.sleep(10);
                        } catch (IOException e) {
                            e.printStackTrace();
                            textView.setText("写入失败，第" + sectorIndex + "扇区，第" + n + "块； " + e.getMessage());
                            close();
                            return;
                        }
                        srcPos += 16;
                    }
                }
            }
            textView.setText("写入完成");
            Toast.makeText(MainActivity.this, "写入完成", Toast.LENGTH_SHORT).show();
            close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (defaultAdapter != null)
            defaultAdapter.enableForegroundDispatch(this, mPendingIntent, null,
                    null);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (isDone) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            tag = patchTag(tag);
            mTag = tag;
            mMFC = MifareClassic.get(tag);
            isDone = false;
        }
    }

    public static Tag patchTag(Tag tag) {
        if (tag == null) {
            return null;
        }

        String[] techList = tag.getTechList();

        Parcel oldParcel = Parcel.obtain();
        tag.writeToParcel(oldParcel, 0);
        oldParcel.setDataPosition(0);

        int len = oldParcel.readInt();
        byte[] id = new byte[0];
        if (len >= 0) {
            id = new byte[len];
            oldParcel.readByteArray(id);
        }
        int[] oldTechList = new int[oldParcel.readInt()];
        oldParcel.readIntArray(oldTechList);
        Bundle[] oldTechExtras = oldParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oldParcel.readInt();
        int isMock = oldParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oldParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oldParcel.recycle();

        int nfcaIdx = -1;
        int mcIdx = -1;
        short sak = 0;
        boolean isFirstSak = true;

        for (int i = 0; i < techList.length; i++) {
            if (techList[i].equals(NfcA.class.getName())) {
                if (nfcaIdx == -1) {
                    nfcaIdx = i;
                }
                if (oldTechExtras[i] != null
                        && oldTechExtras[i].containsKey("sak")) {
                    sak = (short) (sak
                            | oldTechExtras[i].getShort("sak"));
                    isFirstSak = nfcaIdx == i;
                }
            } else if (techList[i].equals(MifareClassic.class.getName())) {
                mcIdx = i;
            }
        }

        boolean modified = false;

        // Patch the double NfcA issue (with different SAK) for
        // Sony Z3 devices.
        if (!isFirstSak) {
            oldTechExtras[nfcaIdx].putShort("sak", sak);
            modified = true;
        }

        // Patch the wrong index issue for HTC One devices.
        if (nfcaIdx != -1 && mcIdx != -1 && oldTechExtras[mcIdx] == null) {
            oldTechExtras[mcIdx] = oldTechExtras[nfcaIdx];
            modified = true;
        }

        if (!modified) {
            // Old tag was not modivied. Return the old one.
            return tag;
        }

        // Old tag was modified. Create a new tag with the new data.
        Parcel newParcel = Parcel.obtain();
        newParcel.writeInt(id.length);
        newParcel.writeByteArray(id);
        newParcel.writeInt(oldTechList.length);
        newParcel.writeIntArray(oldTechList);
        newParcel.writeTypedArray(oldTechExtras, 0);
        newParcel.writeInt(serviceHandle);
        newParcel.writeInt(isMock);
        if (isMock == 0) {
            newParcel.writeStrongBinder(tagService);
        }
        newParcel.setDataPosition(0);
        Tag newTag = Tag.CREATOR.createFromParcel(newParcel);
        newParcel.recycle();

        return newTag;
    }

    private void close() {
        if (mMFC == null) {
            return;
        }
        try {
            mMFC.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    mMFC.connect();
                } catch (IOException | IllegalStateException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 创建密匙键值对
     */
    public void onCreateKeyMap() {
        if (mMFC == null) {
            textView.setText("未发现卡片！");
            return;
        }
//        Toast.makeText(this, "创建密钥映射中… 请稍等…",
//                Toast.LENGTH_LONG).show();
//        textView.setText("创建密钥映射中… 请稍等…");
        connect();
        // Check if key files still exists.
//        ArrayList<File> keyFiles = new ArrayList<>();
//        keyFiles.add(new File(getFilesDir().getAbsolutePath(), "extended-std.keys"));
//        keyFiles.add(new File(Environment.getExternalStorageDirectory().getPath(), "extended-std.keys"));//"/MifareClassicTool/key-files"

//        if (keyFiles.size() > 0) {
//            // Set key files.
//            File[] keys = keyFiles.toArray(new File[keyFiles.size()]);
//            if (!setKeyFile(keys, this)) {
//                // Error.
//                close();
//                return;
//            }
        // Don't turn screen of while mapping.
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Get key map range.

        // 读取全部扇区
        mFirstSector = 0;
        mLastSector = mMFC.getSectorCount() - 1;

        // Set map creation range.
        if (!setMappingRange(mFirstSector, mLastSector)) {
            // Error.
            Toast.makeText(this,
                    "映射范围错误：扇区超出范围。使用更大的标签。",
                    Toast.LENGTH_LONG).show();
            close();
            return;
        }
        // Init. GUI elements.
        mProgressStatus = -1;
        Toast.makeText(this, "创建密钥映射中… 请稍等…",
                Toast.LENGTH_SHORT).show();
        // Read as much as possible with given key file.
        createKeyMap();
//        } else {
//            Toast.makeText(this, "Error: No key files found", Toast.LENGTH_LONG).show();
//        }
    }

    private void createKeyMap() {
        stringBuilder = new StringBuilder();
//        while (mProgressStatus < mLastSector) {
//            mProgressStatus = buildNextKeyMapPart();
//            if (mProgressStatus == -1) {
//                Toast.makeText(this, "映射失败", Toast.LENGTH_LONG).show();
//                stringBuilder.append("映射失败\n");
//                break;
//            }
//        }
        createTagDump();
        stringBuilder.append("\n");
        stringBuilder.append("读取结果：");
        stringBuilder.append("\n");
        stringBuilder.append(hexStr2Str(resultHexStr));
        textView.setText(stringBuilder);
        isDone = true;
        Toast.makeText(MainActivity.this, "读取完成", Toast.LENGTH_SHORT).show();
        close();
    }

    /**
     * 拷贝assets文件到根目录
     *
     * @param savePath
     */
    public static void copyFilesFromAssets(Context myContext, String ASSETS_NAME,
                                           String savePath, String saveName, Context context) {
        String filename = savePath + "/" + saveName;
        File dir = new File(savePath);
        // 如果目录不中存在，创建这个目录
        if (!dir.exists())
            dir.mkdir();
        try {
            if (!(new File(filename)).exists()) {
                InputStream is = myContext.getResources().getAssets().open(ASSETS_NAME);
                FileOutputStream fos = new FileOutputStream(filename);
                byte[] buffer = new byte[7168];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            }
        } catch (Exception e) {
            Toast.makeText(context, "未给demo开发写文件权限", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Set the key files for {@link #buildNextKeyMapPart()}.
     * Key duplicates from the key file will be removed.
     *
     * @param keyFiles One or more key files.
     *                 These files are simple text files with one key
     *                 per line. Empty lines and lines STARTING with "#"
     *                 will not be interpreted.
     * @param context  The context in which the possible "Out of memory"-Toast
     *                 will be shown.
     * @return True if the key files are correctly loaded. False
     * on error (out of memory).
     */
    public boolean setKeyFile(File[] keyFiles, Context context) {
        boolean hasAllZeroKey = false;
        HashSet<byte[]> keys = new HashSet<>();
        for (File file : keyFiles) {
            String[] lines = readFileLineByLine(file, false, context);
            if (lines != null) {
                for (String line : lines) {
                    if (!line.equals("") && line.length() == 12
                            && line.matches("[0-9A-Fa-f]+")) {
                        if (line.equals("000000000000")) {
                            hasAllZeroKey = true;
                        }
                        try {
                            keys.add(hexStringToByteArray(line));
                        } catch (OutOfMemoryError e) {
                            // Error. Too many keys (out of memory).
                            Toast.makeText(context, "错误：太多的密钥(内存不够)",
                                    Toast.LENGTH_LONG).show();
                            return false;
                        }
                    }
                }
            }
        }
        if (keys.size() > 0) {
            mKeysWithOrder = new ArrayList<>(keys);
            byte[] zeroKey = hexStringToByteArray("000000000000");
            if (hasAllZeroKey) {
                // NOTE: The all-F key has to be tested always first if there
                // is a all-0 key in the key file, because of a bug in
                // some tags and/or devices.
                // https://github.com/ikarus23/MifareClassicTool/iss000000000000ues/66
                byte[] fKey = hexStringToByteArray("FFFFFFFFFFFF");
                mKeysWithOrder.remove(fKey);
                mKeysWithOrder.add(0, fKey);
            }
        }
        return true;
    }

    public static String[] readFileLineByLine(File file, boolean readComments, Context context) {
        BufferedReader br = null;
        String[] ret = null;
        if (file != null && file.exists()) {
            try {
                br = new BufferedReader(new FileReader(file));

                String line;
                ArrayList<String> linesArray = new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    // Ignore empty lines.
                    // Ignore comments if readComments == false.
                    if (!line.equals("")
                            && (readComments || !line.startsWith("#"))) {
                        try {
                            linesArray.add(line);
                        } catch (OutOfMemoryError e) {
                            // Error. File is too big
                            // (too many lines, out of memory).
                            Toast.makeText(context, "文件过大！",
                                    Toast.LENGTH_LONG).show();
                            return null;
                        }
                    }
                }
                if (linesArray.size() > 0) {
                    ret = linesArray.toArray(new String[linesArray.size()]);
                } else {
                    ret = new String[]{""};
                }
            } catch (Exception e) {
                Log.e("Error", "Error while reading from file "
                        + file.getPath() + ".", e);
                ret = null;
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.e("Error", "Error while closing file.", e);
                        ret = null;
                    }
                }
            }
        }
        return ret;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        try {
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
        } catch (Exception e) {
            Log.d("Error", "Argument(s) for hexStringToByteArray(String s)"
                    + "was not a hex string");
        }
        return data;
    }

    /**
     * Set the mapping range for {@link #buildNextKeyMapPart()}.
     *
     * @param firstSector Index of the first sector of the key map.
     * @param lastSector  Index of the last sector of the key map.
     * @return True if range parameters were correct. False otherwise.
     */
    public boolean setMappingRange(int firstSector, int lastSector) {
        if (firstSector >= 0 && lastSector < mMFC.getSectorCount()
                && firstSector <= lastSector) {
            mFirstSector = firstSector;
            mLastSector = lastSector;
            // Init. status of buildNextKeyMapPart to create a new key map.
            mKeyMapStatus = lastSector + 1;
            return true;
        }
        return false;
    }

    /**
     * Build Key-Value Pairs in which keys represent the sector and
     * values are one or both of the MIFARE keys (A/B).
     * The MIFARE key information must be set before calling this method
     * (use {@link #setKeyFile(File[], Context)}).
     * Also the mapping range must be specified before calling this method
     * (use {@link #setMappingRange(int, int)}).<br /><br />
     * The mapping works like some kind of dictionary attack.
     * All keys are checked against the next sector
     * with both authentication methods (A/B). If at least one key was found
     * for a sector, the map will be extended with an entry, containing the
     * key(s) and the information for what sector the key(s) are. You can get
     * this Key-Value Pairs by calling . A full
     * key map can be gained by calling this method as often as there are
     * sectors on the tag  If you call
     * this method once more after a full key map was created, it resets the
     * key map and starts all over.
     *
     * @return The sector that was just checked. On an error condition,
     * it returns "-1" and resets the key map to "null".
     */
    public int buildNextKeyMapPart() {
        // Clear status and key map before new walk through sectors.
        boolean error = false;
        int retryAuthCount = 1;
        if (mKeysWithOrder != null && mLastSector != -1) {
            if (mKeyMapStatus == mLastSector + 1) {
                mKeyMapStatus = mFirstSector;
                mKeyMap = new SparseArray<>();
            }

            byte[][] keys = new byte[2][];
            boolean[] foundKeys = new boolean[]{false, false};
            boolean auth = false;

            // Check next sector against all keys (lines) with
            // authentication method A and B.
            keysloop:
            for (int i = 0; i < mKeysWithOrder.size(); i++) {
                byte[] key = mKeysWithOrder.get(i);
                for (int j = 0; j < retryAuthCount + 1; ) {
                    try {
                        if (!foundKeys[0]) {
                            auth = mMFC.authenticateSectorWithKeyA(
                                    mKeyMapStatus, key);
                            if (auth) {
                                keys[0] = key;
                                foundKeys[0] = true;
                            }
                        }
                        if (!foundKeys[1]) {
                            auth = mMFC.authenticateSectorWithKeyB(
                                    mKeyMapStatus, key);
                            if (auth) {
                                keys[1] = key;
                                foundKeys[1] = true;
                            }
                        }
                        Log.e("auth" + mProgressStatus, auth == true ? "true" : "false");
                    } catch (Exception e) {
                        Log.d("Error",
                                "Error while building next key map part");
                        if (true) {
                            Log.d("Error", "Auto reconnect is enabled");
                            while (!mMFC.isConnected()) {
                                // Sleep for 500ms.
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException ex) {
                                    // Do nothing.
                                }
                                // Try to reconnect.
                                try {
                                    connect();
                                } catch (Exception ex) {
                                    // Do nothing.
                                }
                            }
                            // Repeat last loop (do not incr. j).
                            continue;
                        } else {
                            error = true;
                            break keysloop;
                        }
                    }
                    // Retry?
                    if ((foundKeys[0] && foundKeys[1])) {
                        // Both keys found or no retry wanted. Stop retrying.
                        break;
                    }
                    j++;
                }
                // Next key?
                if ((foundKeys[0] && foundKeys[1])) {
                    // Both keys found. Stop searching for keys.
                    break;
                }
            }
            if (!error && (foundKeys[0] || foundKeys[1])) {
                // At least one key found. Add key(s).
                mKeyMap.put(mKeyMapStatus, keys);
                // Key reuse is very likely, so try the found keys second.
                // NOTE: The all-F key has to be tested always first if there
                // is a all-0 key in the key file, because of a bug in
                // some tags and/or devices.
                // https://github.com/ikarus23/MifareClassicTool/issues/66
                byte[] fKey = hexStringToByteArray("FFFFFFFFFFFF");
                if (mKeysWithOrder.size() > 2) {
                    if (foundKeys[0] && !Arrays.equals(keys[0], fKey)) {
                        mKeysWithOrder.remove(keys[0]);
                        mKeysWithOrder.add(1, keys[0]);
                    }
                    if (foundKeys[1] && !Arrays.equals(keys[1], fKey)) {
                        mKeysWithOrder.remove(keys[1]);
                        mKeysWithOrder.add(1, keys[1]);
                    }
                }
            }
            mKeyMapStatus++;
        } else {
            error = true;
        }

        if (error) {
            mKeyMapStatus = 0;
            mKeyMap = null;
            return -1;
        }
        return mKeyMapStatus - 1;
    }

    /**
     * Create a tag dump in a format the
     * can read (format: headers (sectors) marked with "+", errors
     * marked with "*"), and then start the dump editor with this dump.
     * returns.
     */
    private void createTagDump() {
        SparseArray<String[]> rawDump = readAsMuchAsPossible(mKeyMap);
        ArrayList<String> tmpDump = new ArrayList<>();
        if (rawDump != null) {
            if (rawDump.size() != 0) {
                for (int i = mFirstSector; i <= mLastSector; i++) {
                    String[] val = rawDump.get(i);
                    // Mark headers (sectors) with "+".
                    tmpDump.add("+Sector: " + i);
                    if (val != null) {
                        Collections.addAll(tmpDump, val);
                        stringBuilder.append("扇区" + i + "读取成功！\n");
                        for (int n = 0; n < val.length; n++) {
                            stringBuilder.append(val[n] + "\n");
                            if (i > 31 && n != 15 && !val[n].equals("00000000000000000000000000000000")) {
                                resultHexStr += val[n];
                            }
                            if (i < 32 && n != 3 && !val[n].equals("00000000000000000000000000000000")) {
                                if (!(i == 0 && n == 0)) {
                                    resultHexStr += val[n];
                                }
                            }
                        }
                        stringBuilder.append("\n");
                    } else {
                        // Mark sector as not readable ("*").
                        stringBuilder.append("扇区" + i + "读取失败 ********************\n");
                        stringBuilder.append("\n");
                        tmpDump.add("*No keys found or dead sector");
                    }
                }
            } else {
                // Error, keys from key map are not valid for reading.
                Toast.makeText(this, "该区无密匙",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "卡片以移开",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Read as much as possible from the tag with the given key information.
     *
     * @param keyMap Keys (A and B) mapped to a sector.
     *               See {@link #buildNextKeyMapPart()}.
     * @return A Key-Value Pair. Keys are the sector numbers, values
     * are the tag data. This tag data (values) are arrays containing
     * one block per field (index 0-3 or 0-15).
     * If a block is "null" it means that the block couldn't be
     * read with the given key information.<br />
     * On Error, "null" will be returned (tag was removed during reading or
     * keyMap is null). If none of the keys in the key map are valid for reading
     * (and therefore no sector is read), an empty set (SparseArray.size() == 0)
     * will be returned.
     * @see #buildNextKeyMapPart()
     */
    public SparseArray<String[]> readAsMuchAsPossible(SparseArray<byte[][]> keyMap) {
        SparseArray<String[]> resultSparseArray;
        resultSparseArray = new SparseArray<>(40);
        for (int i = 0; i < 40; i++) {
            String[][] results = new String[2][];
            try {
                // Read with key A.
                results[0] = readSector(i);
                //防止读取失败
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (results[0] == null) {
                    //失败重读
                    Log.e("读取失败A", i + "区");
                    for (int i2 = 0; i2 < 4; i2++) {

                        results[0] = readSector(i);

                        if (results[0] != null) {
                            Log.e("重新读取成功A", i + "区");
                            break;
                        }
                    }
                }
                if (results[0] != null || results[1] != null) {
                    resultSparseArray.put(i, mergeSectorData(
                            results[0], results[1]));
                }
            } catch (Exception e) {
                return null;
            }
        }

//        if (keyMap != null && keyMap.size() > 0) {
//            resultSparseArray = new SparseArray<>(keyMap.size());
//            // For all entries in map do:
//            for (int i = 0; i < keyMap.size(); i++) {
//                String[][] results = new String[2][];
//                try {
//                    if (keyMap.valueAt(i)[0] != null) {
//                        // Read with key A.
//                        results[0] = readSector(
//                                keyMap.keyAt(i), keyMap.valueAt(i)[0], false);
//                        //防止读取失败
//                        try {
//                            Thread.sleep(10);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        if (results[0] == null) {
//                            //失败重读
//                            Log.e("读取失败A", i + "区");
//                            for (int i2 = 0; i2 < 4; i2++) {
//                                results[0] = readSector(
//                                        keyMap.keyAt(i), keyMap.valueAt(i)[0], false);
//                                if (results[0] != null) {
//                                    Log.e("重新读取成功A", i + "区");
//                                    break;
//                                }
//                            }
//                        }
//                    }else if (keyMap.valueAt(i)[1] != null) {
//                        // Read with key B.
//                        results[1] = readSector(
//                                keyMap.keyAt(i), keyMap.valueAt(i)[1], true);
//                        //防止读取失败
//                        try {
//                            Thread.sleep(10);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//
//                        if (results[1] == null) {
//                            //失败重读
//                            Log.e("读取失败B", i + "区");
//                            for (int i2 = 0; i2 < 2; i2++) {
//                                results[1] = readSector(
//                                        keyMap.keyAt(i), keyMap.valueAt(i)[1], false);
//                                if (results[1] != null) {
//                                    Log.e("重新读取成功B", i + "区");
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                } catch (TagLostException e) {
//                    return null;
//                }
//                // Merge results.
//                if (results[0] != null || results[1] != null) {
//                    resultSparseArray.put(keyMap.keyAt(i), mergeSectorData(
//                            results[0], results[1]));
//                }
//            }
//            return resultSparseArray;
//        }
//        return null;
        return resultSparseArray;
    }

    /**
     * Merge the result of two {@link #readSector(int, byte[], boolean)}
     * calls on the same sector (with different keys or authentication methods).
     * In this case merging means empty blocks will be overwritten with non
     * empty ones and the keys will be added correctly to the sector trailer.
     * The access conditions will be taken from the first (firstResult)
     * parameter if it is not null.
     *
     * @param firstResult  First
     *                     {@link #readSector(int, byte[], boolean)} result.
     * @param secondResult Second
     *                     {@link #readSector(int, byte[], boolean)} result.
     * @return Array (sector) as result of merging the given
     * sectors. If a block is {@link #NO_DATA} it
     * means that none of the given sectors contained data from this block.
     * @see #readSector(int, byte[], boolean)
     * @see #authenticate(int, byte[], boolean)
     */
    public String[] mergeSectorData(String[] firstResult,
                                    String[] secondResult) {
        String[] ret = null;
        if (firstResult != null || secondResult != null) {
            if ((firstResult != null && secondResult != null)
                    && firstResult.length != secondResult.length) {
                return null;
            }
            int length = (firstResult != null)
                    ? firstResult.length : secondResult.length;
            ArrayList<String> blocks = new ArrayList<>();
            // Merge data blocks.
            for (int i = 0; i < length - 1; i++) {
                if (firstResult != null && firstResult[i] != null
                        && !firstResult[i].equals(NO_DATA)) {
                    blocks.add(firstResult[i]);
                } else if (secondResult != null && secondResult[i] != null
                        && !secondResult[i].equals(NO_DATA)) {
                    blocks.add(secondResult[i]);
                } else {
                    // None of the results got the data form the block.
                    blocks.add(NO_DATA);
                }
            }
            ret = blocks.toArray(new String[blocks.size() + 1]);
            int last = length - 1;
            // Merge sector trailer.
            if (firstResult != null && firstResult[last] != null
                    && !firstResult[last].equals(NO_DATA)) {
                // Take first for sector trailer.
                ret[last] = firstResult[last];
                if (secondResult != null && secondResult[last] != null
                        && !secondResult[last].equals(NO_DATA)) {
                    // Merge key form second result to sector trailer.
                    ret[last] = ret[last].substring(0, 20)
                            + secondResult[last].substring(20);
                }
            } else if (secondResult != null && secondResult[last] != null
                    && !secondResult[last].equals(NO_DATA)) {
                // No first result. Take second result as sector trailer.
                ret[last] = secondResult[last];
            } else {
                // No sector trailer at all.
                ret[last] = NO_DATA;
            }
        }
        return ret;
    }

    String blockStr = "";

    /**
     * Read as much as possible from a sector with the given key.
     * Best results are gained from a valid key B (except key B is marked as
     * readable in the access conditions).
     *
     * @param sectorIndex Index of the Sector to read. (For MIFARE Classic 1K:
     *                    0-63)
     * @param key         Key for authentication.
     * @param useAsKeyB   If true, key will be treated as key B
     *                    for authentication.
     * @return Array of blocks (index 0-3 or 0-15). If a block or a key is
     * marked with {@link #NO_DATA} or {@link #NO_KEY}
     * it means that this data could not be read or found. On authentication error
     * "null" will be returned.
     * @throws TagLostException When connection with/to tag is lost.
     */
    public String[] readSector(int sectorIndex/*, byte[] key,
                               boolean useAsKeyB*/) throws TagLostException {
        boolean auth = authenticate(sectorIndex, hexStringToByteArray(keyA2), false);
        if (!auth) {
            auth = authenticate(sectorIndex, hexStringToByteArray(keyA1), false);
            if (!auth) {
                auth = authenticate(sectorIndex, hexStringToByteArray(keyB), true);
            }
        }
//        boolean auth = authenticate(sectorIndex, key, useAsKeyB);
        String[] ret = null;
        // Read sector.
        if (auth) {
            // Read all blocks.
            ArrayList<String> blocks = new ArrayList<>();
            int firstBlock = mMFC.sectorToBlock(sectorIndex);
            int lastBlock = firstBlock + 4;
            if (mMFC.getSize() == MifareClassic.SIZE_4K
                    && sectorIndex > 31) {
                lastBlock = firstBlock + 16;
            }
            for (int i = firstBlock; i < lastBlock; i++) {
                try {
                    byte blockBytes[] = mMFC.readBlock(i);
                    // mMFC.readBlock(i) must return 16 bytes or throw an error.
                    // At least this is what the documentation says.
                    // On Samsung's Galaxy S5 and Sony's Xperia Z2 however, it
                    // sometimes returns < 16 bytes for unknown reasons.
                    // Update: Aaand sometimes it returns more than 16 bytes...
                    // The appended byte(s) are 0x00.
                    if (blockBytes.length < 16) {
                        throw new IOException();
                    }
                    if (blockBytes.length > 16) {
                        blockBytes = Arrays.copyOf(blockBytes, 16);
                    }

                    blocks.add(byte2HexString(blockBytes));
                    blockStr = byte2HexString(blockBytes);
                    try {
                        Log.e("单行读取结果block:" + i, hexStr2Str(blockStr));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("单行读取结果失败", e.getMessage());
                    }
                } catch (TagLostException e) {
                    throw e;
                } catch (IOException e) {
                    // Could not read block.
                    // (Maybe due to key/authentication method.)
                    Log.d("Error", "(Recoverable) Error while reading block "
                            + i + " from tag.");
                    blocks.add(NO_DATA);
                    if (!mMFC.isConnected()) {
                        throw new TagLostException(
                                "Tag removed during readSector(...)");
                    }
                    // After an error, a re-authentication is needed.
//                    authenticate(sectorIndex, key, useAsKeyB);
                    authenticate(sectorIndex, hexStringToByteArray(keyA1), false);
                }
            }
            ret = blocks.toArray(new String[blocks.size()]);
            int last = ret.length - 1;

            // Validate if it was possible to read any data.
            boolean noData = true;
            for (int i = 0; i < ret.length; i++) {
                if (!ret[i].equals(NO_DATA)) {
                    noData = false;
                    break;
                }
            }
            if (noData) {
                // Was is possible to read any data (especially with key B)?
                // If Key B may be read in the corresponding Sector Trailer,
                // it cannot serve for authentication (according to NXP).
                // What they mean is that you can authenticate successfully,
                // but can not read data. In this case the
                // readBlock() result is 0 for each block.
                // Also, a tag might be bricked in a way that the authentication
                // works, but reading data does not.
                ret = null;
            } else {
                // Merge key in last block (sector trailer).
//                if (!useAsKeyB) {
//                    if (isKeyBReadable(hexStringToByteArray(
//                            ret[last].substring(12, 20)))) {
//                        ret[last] = byte2HexString(key)
//                                + ret[last].substring(12, 32);
//                    } else {
//                        ret[last] = byte2HexString(key)
//                                + ret[last].substring(12, 20) + NO_KEY;
//                    }
//                } else {
//                    ret[last] = NO_KEY + ret[last].substring(12, 20)
//                            + byte2HexString(key);
//                }
            }
        }
        return ret;
    }


    /**
     * 16进制直接转换成为字符串(无需Unicode解码)
     *
     * @param hexStr
     * @return
     */
    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    /**
     * Check if key B is readable.
     * Key B is readable for the following configurations:
     * <ul>
     * <li>C1 = 0, C2 = 0, C3 = 0</li>
     * <li>C1 = 0, C2 = 0, C3 = 1</li>
     * <li>C1 = 0, C2 = 1, C3 = 0</li>
     * </ul>
     *
     * @param ac The access conditions (4 bytes).
     * @return True if key B is readable. False otherwise.
     */
    private boolean isKeyBReadable(byte[] ac) {
        byte c1 = (byte) ((ac[1] & 0x80) >>> 7);
        byte c2 = (byte) ((ac[2] & 0x08) >>> 3);
        byte c3 = (byte) ((ac[2] & 0x80) >>> 7);
        return c1 == 0
                && (c2 == 0 && c3 == 0)
                || (c2 == 1 && c3 == 0)
                || (c2 == 0 && c3 == 1);
    }

    /**
     * 使用标记的给定扇区进行身份验证。
     *
     * @param sectorIndex 要进行身份验证的扇区。
     * @param key         验证密钥。
     * @param useAsKeyB   如果为true，则键将被视为键B. 用于身份验证。
     * @return如果验证成功，则为True。否则就错了。
     */
    private boolean authenticate(int sectorIndex, byte[] key,
                                 boolean useAsKeyB) {
        // Fetch the retry authentication option. Some tags and
        // devices have strange issues and need a retry in order to work...
        // Info: https://github.com/ikarus23/MifareClassicTool/issues/134
        // and https://github.com/ikarus23/MifareClassicTool/issues/106
        boolean retryAuth = false;
        int retryCount = 1;
        boolean ret = false;
        for (int i = 0; i < retryCount + 1; i++) {
            try {
                if (!mMFC.isConnected()) {
                    connect();
                }
                if (!useAsKeyB) {
                    // Key A.
                    if (mMFC.isConnected()) {
                        ret = mMFC.authenticateSectorWithKeyA(sectorIndex, key);
                    }
                } else {
                    // Key B.
                    if (mMFC.isConnected()) {
                        ret = mMFC.authenticateSectorWithKeyB(sectorIndex, key);
                    }
                }
            } catch (IOException e) {
                Log.d("Error", "Error authenticating with tag.");
                return false;
            }
            // Retry?
            if (ret || !retryAuth) {
                break;
            }
        }
        return ret;
    }

    /**
     * Convert an array of bytes into a string of hex values.
     *
     * @param bytes Bytes to convert.
     * @return The bytes in hex string format.
     */
    public static String byte2HexString(byte[] bytes) {
        StringBuilder ret = new StringBuilder();
        if (bytes != null) {
            for (Byte b : bytes) {
                ret.append(String.format("%02X", b.intValue() & 0xFF));
            }
        }
        return ret.toString();
    }

}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      