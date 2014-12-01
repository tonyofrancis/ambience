package com.tonyostudios.ambience;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;


/**
 * This class holds track information. It is passed to Ambience and the AmbientService
 * for playback.
 * @author TonyoStudios.com. Created on 11/18/2014. Updated on 11/27/2014.
 * @version 1.3
 */
public class AmbientTrack implements Parcelable {

    public static final String TAG = "com.tonyostudios.ambience.AmbientTrack";

    //PRIVATE TRACK FIELDS
    private long id = -1;
    private String name = "";
    private int duration = -1;
    private String artistName = "";
    private long artistId = -1;
    private String albumName = "";
    private long albumId = -1;
    private int position = -1;
    private String releaseDate = "";
    private Uri albumImageUri = Uri.EMPTY;
    private Uri audioUri = Uri.EMPTY;
    private Uri audioDownloadUri = Uri.EMPTY;
    private ArrayList<String> genresList = new ArrayList<String>();

    /**
     * Private Constructor
     */
    private AmbientTrack()
    {
        super();
    }

    /**
     * Method used to get a new instance of AmbientTrack
     * @return new instance of AmbientTrack
     */
    public static AmbientTrack newInstance()
    {
        return new AmbientTrack();
    }

    // Parcel creator object
    public static final Creator<AmbientTrack> CREATOR = new Creator<AmbientTrack>() {

        // Method used to create an AmbientTrack from Parcel
        @Override
        public AmbientTrack createFromParcel(Parcel in) {

            AmbientTrack AmbientTrack = new AmbientTrack();

            AmbientTrack.id = in.readLong();
            AmbientTrack.name = in.readString();
            AmbientTrack.duration = in.readInt();
            AmbientTrack.artistName =in.readString();
            AmbientTrack.artistId = in.readLong();
            AmbientTrack.albumName = in.readString();
            AmbientTrack.albumId = in.readLong();
            AmbientTrack.position = in.readInt();
            AmbientTrack.releaseDate = in.readString();
            AmbientTrack.albumImageUri = Uri.parse(in.readString());
            AmbientTrack.audioUri = Uri.parse(in.readString());
            AmbientTrack.audioDownloadUri = Uri.parse(in.readString());
            in.readStringList(AmbientTrack.genresList);

            return AmbientTrack;
        }

        /**
         * Method used to return an AmbientTrack Array
         * @param i Array size
         * @return AmbientTrack Array
         */
        @Override
        public AmbientTrack[] newArray(int i) {
            return new AmbientTrack[i];
        }
    };

    /**
     * Method that describes parcel content
     * @return always returns 0 (Default)
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Method used to Create Parcel from AmbientTrack Object
     * @param parcel Parcel item
     * @param i Flag
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeLong(id);
        parcel.writeString(name);
        parcel.writeInt(duration);
        parcel.writeString(artistName);
        parcel.writeLong(artistId);
        parcel.writeString(albumName);
        parcel.writeLong(albumId);
        parcel.writeInt(position);
        parcel.writeString(releaseDate);
        parcel.writeString(albumImageUri.toString());
        parcel.writeString(audioUri.toString());
        parcel.writeString(audioDownloadUri.toString());
        parcel.writeStringList(genresList);

    }

    /**
     * Method that returns track id
     * @return Track id
     */
    public long getId() {
        return id;
    }

    /**
     *Method that returns track name
     * @return Track name
     */
    public String getName() {
        return name;
    }

    /**
     * Method that returns track duration
     * @return Track duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Method that returns artist name
     * @return Artist name
     */
    public String getArtistName() {
        return artistName;
    }

    /**
     * Method that returns artist id
     * @return Artist id
     */
    public long getArtistId() {
        return artistId;
    }

    /**
     * Method that returns album name
     * @return Album Name
     */
    public String getAlbumName() {
        return albumName;
    }

    /**
     * Method that returns album id
     * @return Album id
     */
    public long getAlbumId() {
        return albumId;
    }

    /**
     * Method that returns track position
     * @return Track position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Method that returns track release date
     * @return Track releaseDate
     */
    public String getReleaseDate() {
        return releaseDate;
    }

    /**
     * Method that returns uri where album image is located
     * @return String uri where album image is located
     */
    public Uri getAlbumImageUri() {
        return albumImageUri;
    }

    /**
     * Method that returns uri where track is located
     * @return String uri where track is located
     */
    public Uri getAudioUri() {
        return audioUri;
    }

    /**
     * Method that returns uri where track can be downloaded
     * @return String uri where track can be downloaded
     */
    public Uri getAudioDownloadUri() {
        return audioDownloadUri;
    }

    /**
     * Method that gets track genres
     * @return String array of track genres
     */
    public ArrayList<String> getGenresList() {
        return genresList;
    }

    /**
     * Method that sets track id
     * @param id Track id
     * @return The AmbientTrack instance
     */
    public AmbientTrack setId(long id) {
        this.id = id;

        return this;
    }

    /**
     * Method that sets track name
     * @param name Track name
     * @return The AmbientTrack instance
     */
    public AmbientTrack setName(String name) {

        if(name == null)
        {
            return this;
        }

        this.name = name;

        return this;
    }

    /**
     * Method that sets track duration
     * @param duration Track duration
     * @return The AmbientTrack instance
     */
    public AmbientTrack setDuration(int duration) {
        this.duration = duration;

        return this;
    }

    /**
     * Method that sets artist name
     * @param artistName Artist name
     * @return The AmbientTrack instance
     */
    public AmbientTrack setArtistName(String artistName) {

        if(artistName == null)
        {
            return this;
        }

        this.artistName = artistName;

        return this;
    }

    /**
     * Method that sets artist id
     * @param artistId Artist id
     * @return The AmbientTrack instance
     */
    public AmbientTrack setArtistId(long artistId) {
        this.artistId = artistId;

        return this;
    }

    /**
     * Method that sets album name
     * @param albumName Album Name
     * @return The AmbientTrack instance
     */
    public AmbientTrack setAlbumName(String albumName) {

        if(albumName == null)
        {
            return this;
        }

        this.albumName = albumName;

        return this;
    }

    /**
     * Method that sets album id
     * @param albumId Album id
     * @return The AmbientTrack instance
     */
    public AmbientTrack setAlbumId(long albumId) {
        this.albumId = albumId;

        return this;
    }

    /**
     * Method that sets track position
     * @param position Track position
     * @return The AmbientTrack instance
     */
    public AmbientTrack setPosition(int position) {
        this.position = position;

        return this;
    }

    /**
     * Method that sets track release date
     * @param releaseDate Track release date
     * @return The AmbientTrack instance
     */
    public AmbientTrack setReleaseDate(String releaseDate) {

        if(releaseDate == null)
        {
            return this;
        }

        this.releaseDate = releaseDate;

        return this;
    }

    /**
     * Method that sets album image uri where the album image is located
     * @param albumImage String uri where the track album is located
     * @return The AmbientTrack instance
     */
    public AmbientTrack setAlbumImageUri(Uri albumImage) {

        if(albumImage == null)
        {
            return this;
        }

        this.albumImageUri = albumImage;

        return this;
    }

    /**
     * Method that sets track uri where track is located
     * @param audio String uri where the track is located
     * @return The AmbientTrack instance
     */
    public AmbientTrack setAudioUri(Uri audio) {

        if(audio == null)
        {
            return this;
        }

        this.audioUri = audio;

        return this;
    }

    /**
     * Method that sets track uri where track can be downloaded
     * @param audioDownload String uri where the track can be downloaded
     * @return The AmbientTrack instance
     */
    public AmbientTrack setAudioDownloadUri(Uri audioDownload) {

        if(audioDownload == null)
        {
            return this;
        }

        this.audioDownloadUri = audioDownload;

        return this;
    }

    /**
     * Method that sets track genres
     * @param genres String array of track genres
     * @return The AmbientTrack instance
     */
    public AmbientTrack setGenres(ArrayList<String> genres) {

        if(genres == null)
        {
            return this;
        }

        for(int x = 0; x < genres.size(); x++)
        {
            if(genres.get(x) == null)
            {
                throw new NullPointerException(TAG +": cannot contain a null object in the genre list");
            }
        }

        this.genresList = genres;

        return this;
    }
}
