package com.example.joeyt.rssreader;

public class RSSItem {

    private CharSequence mTitle;
    private CharSequence mLink;
    private CharSequence mDescription;
    private CharSequence mImage;

    public RSSItem() {
        mTitle = "";
        mLink = "";
        mDescription = "";
        mImage = "";
    }

    public RSSItem(CharSequence title, CharSequence link, CharSequence description, CharSequence image) {
        mTitle = title;
        mLink = link;
        mDescription = description;
        mImage = image;
    }

    public CharSequence getmImage() {
        return mImage;
    }

    public void setmImage(CharSequence image) {
        mImage = image;
    }

    public CharSequence getDescription() {
        return mDescription;
    }

    public void setDescription(CharSequence description) {
        mDescription = description;
    }

    public CharSequence getLink() {
        return mLink;
    }

    public void setLink(CharSequence link) {
        mLink = link;
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
    }
}