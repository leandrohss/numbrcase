package com.numbrcase.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.numbrcase.common.SocialMediaIDs;
import com.numbrcase.dal.ContactDB;
import com.numbrcase.model.Contact;
import com.numbrcase.model.ContactImpl;
import com.numbrcase.model.MediaArrayAdapter;
import com.numbrcase.model.SocialMedia;
import com.numbrcase.model.SocialMediaImpl;
import com.test_2.R;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddContactActivity extends AppCompatActivity {

    private ListView listview;

    private List<SocialMedia> socialMedias = new ArrayList<>();

    private Contact contact = new ContactImpl();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        listview = (ListView) findViewById(R.id.social_medias_list_view);
        MediaArrayAdapter adapter = new MediaArrayAdapter(this, new ArrayList<SocialMedia>(), R.layout.row_add_media);
        listview.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_contact, menu);
        return true;
    }

    /**
     * Method called whenever the button "Add Social Media" is pressed
     */
    public void openSocialMedias(View view) {
        PopupMenu popup = new PopupMenu(this, view);

        // Populate social_media_menu dinamically (maybe later)
        List<Integer> mediaIDs = SocialMediaIDs.getSocialMediaIDs();
        for (int i = 0; i < mediaIDs.size(); i++) {
            boolean mediaNotAlreadyAdded = true;

            if (listview != null) {
                for (int j = 0; j < listview.getAdapter().getCount(); j++) {
                    // If the social media have already been added into the list
                    if (mediaIDs.get(i) == ((SocialMediaImpl) listview.getAdapter().getItem(j)).getMediaID()) {
                        mediaNotAlreadyAdded = false;
                        break;
                    }
                }
            }

            // If the media was not added or the listview was not created yet
            if (listview == null || mediaNotAlreadyAdded)
                popup.getMenu().add(0, mediaIDs.get(i), 0, SocialMediaIDs.getName(mediaIDs.get(i)));
        }

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.social_media_menu, popup.getMenu());
        popup.show();

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                socialMedias.add(new SocialMediaImpl(item.getItemId(), ""));
                addSocialMedias();
                return true;
            }
        });
    }


    public void addSocialMedias() {
        MediaArrayAdapter adapter = new MediaArrayAdapter(this, socialMedias, R.layout.row_add_media);
        listview.setAdapter(adapter);

        updateListViewSize(listview);
    }

    /**
     * Method called whenever the button "Save" is pressed
     */
    public void saveContact(MenuItem item){
        ContactDB db = new ContactDB(this);

        EditText etName   = (EditText) findViewById(R.id.contact_name);
        EditText etPhone  = (EditText) findViewById(R.id.phone_number);
        EditText etEmail  = (EditText) findViewById(R.id.email);

        ListView lvMedias = (ListView) findViewById(R.id.social_medias_list_view);

        for (int i = 0; i < lvMedias.getAdapter().getCount(); i++) {

            List<View> views  = lvMedias.getTouchables();
            String userID = ((EditText) views.get(i+i+1)).getText().toString();

            // Add only medias with userID setted
            if (userID.equals(""))
                continue;
            else {
                socialMedias.get(i).setUserID(userID);
                socialMedias.get(i).setContactID(db.numberOfRows()+1);
            }
        }

        contact.setName(etName.getText().toString());
        contact.setPhone(etPhone.getText().toString());
        contact.setEmail(etEmail.getText().toString());
        contact.setRequestPlace("");
        contact.setStatus(Contact.ADDED);
        contact.setSocialMedias(socialMedias);

        if (contact.getProfilePicture() == null)
            contact.setProfilePicture(getProfilePic(R.drawable.pp_1));

        db.insertContact(contact);

        Toast.makeText(this, "Contact Saved", Toast.LENGTH_SHORT).show();
        this.onBackPressed(); //Back to MainActivity
    }

    /**
     * Method called whenever the button "Cancel" is pressed
     */
    public void cancel(MenuItem item){
        onBackPressed(); //Back to MainActivity
    }


    /**
     * Method required to expand a ListView since Android do not support a ListView inside
     * a ScrollView
     */
    private void updateListViewSize(ListView listview) {
        int totalHeight = 0;
        for (int i = 0; i < listview.getAdapter().getCount(); i++) {
            View listItem = listview.getAdapter().getView(i, null, listview);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listview.getLayoutParams();
        params.height = totalHeight + (listview.getDividerHeight() * (listview.getCount() - 1));
        listview.setLayoutParams(params);
        listview.requestLayout();

    }

    private byte[] getProfilePic(int drawableID) {
        Bitmap image = BitmapFactory.decodeResource(getResources(),
                drawableID);

        // convert bitmap to byte
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.WEBP, 50, stream);

        return stream.toByteArray();
    }

    /**
     * Method called whenever the button "add_photo" is pressed
     */
    public void changeProfilePicture(View view) {
        PopupMenu popup = new PopupMenu(this, view);

        popup.getMenu().add(0, 0, 0, "Choose from Gallery");
        popup.getMenu().add(0, 1, 1, "Take Phoro");

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.social_media_menu, popup.getMenu());
        popup.show();

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == 0) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, 0);
                }
                else if (item.getItemId() == 1) {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.putExtra("aspectX", 1);
                    cameraIntent.putExtra("aspectY", 1);
                    cameraIntent.putExtra("scale", true);

                    startActivityForResult(cameraIntent, 1);
                }

                return true;
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null)
            return;

        try {

            InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
            Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(inputStream), 256, 256, true);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 75, stream);

            contact.setProfilePicture(stream.toByteArray());
            ((ImageView)findViewById(R.id.add_photo)).setImageBitmap(bitmap);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


}
