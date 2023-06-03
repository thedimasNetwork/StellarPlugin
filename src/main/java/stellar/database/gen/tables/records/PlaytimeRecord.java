/*
 * This file is generated by jOOQ.
 */
package stellar.database.gen.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record14;
import org.jooq.Row14;
import org.jooq.impl.UpdatableRecordImpl;

import stellar.database.gen.tables.Playtime;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PlaytimeRecord extends UpdatableRecordImpl<PlaytimeRecord> implements Record14<String, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>mindustry.playtime.uuid</code>.
     */
    public void setUuid(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>mindustry.playtime.uuid</code>.
     */
    public String getUuid() {
        return (String) get(0);
    }

    /**
     * Setter for <code>mindustry.playtime.hub</code>.
     */
    public void setHub(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>mindustry.playtime.hub</code>.
     */
    public Long getHub() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>mindustry.playtime.survival</code>.
     */
    public void setSurvival(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>mindustry.playtime.survival</code>.
     */
    public Long getSurvival() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>mindustry.playtime.attack</code>.
     */
    public void setAttack(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>mindustry.playtime.attack</code>.
     */
    public Long getAttack() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>mindustry.playtime.sandbox</code>.
     */
    public void setSandbox(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>mindustry.playtime.sandbox</code>.
     */
    public Long getSandbox() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>mindustry.playtime.pvp</code>.
     */
    public void setPvp(Long value) {
        set(5, value);
    }

    /**
     * Getter for <code>mindustry.playtime.pvp</code>.
     */
    public Long getPvp() {
        return (Long) get(5);
    }

    /**
     * Setter for <code>mindustry.playtime.annexation</code>.
     */
    public void setAnnexation(Long value) {
        set(6, value);
    }

    /**
     * Getter for <code>mindustry.playtime.annexation</code>.
     */
    public Long getAnnexation() {
        return (Long) get(6);
    }

    /**
     * Setter for <code>mindustry.playtime.anarchy</code>.
     */
    public void setAnarchy(Long value) {
        set(7, value);
    }

    /**
     * Getter for <code>mindustry.playtime.anarchy</code>.
     */
    public Long getAnarchy() {
        return (Long) get(7);
    }

    /**
     * Setter for <code>mindustry.playtime.campaign_maps</code>.
     */
    public void setCampaignMaps(Long value) {
        set(8, value);
    }

    /**
     * Getter for <code>mindustry.playtime.campaign_maps</code>.
     */
    public Long getCampaignMaps() {
        return (Long) get(8);
    }

    /**
     * Setter for <code>mindustry.playtime.ms_go</code>.
     */
    public void setMsGo(Long value) {
        set(9, value);
    }

    /**
     * Getter for <code>mindustry.playtime.ms_go</code>.
     */
    public Long getMsGo() {
        return (Long) get(9);
    }

    /**
     * Setter for <code>mindustry.playtime.hex_pvp</code>.
     */
    public void setHexPvp(Long value) {
        set(10, value);
    }

    /**
     * Getter for <code>mindustry.playtime.hex_pvp</code>.
     */
    public Long getHexPvp() {
        return (Long) get(10);
    }

    /**
     * Setter for <code>mindustry.playtime.castle_wars</code>.
     */
    public void setCastleWars(Long value) {
        set(11, value);
    }

    /**
     * Getter for <code>mindustry.playtime.castle_wars</code>.
     */
    public Long getCastleWars() {
        return (Long) get(11);
    }

    /**
     * Setter for <code>mindustry.playtime.crawler_arena</code>.
     */
    public void setCrawlerArena(Long value) {
        set(12, value);
    }

    /**
     * Getter for <code>mindustry.playtime.crawler_arena</code>.
     */
    public Long getCrawlerArena() {
        return (Long) get(12);
    }

    /**
     * Setter for <code>mindustry.playtime.zone_capture</code>.
     */
    public void setZoneCapture(Long value) {
        set(13, value);
    }

    /**
     * Getter for <code>mindustry.playtime.zone_capture</code>.
     */
    public Long getZoneCapture() {
        return (Long) get(13);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record14 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row14<String, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long> fieldsRow() {
        return (Row14) super.fieldsRow();
    }

    @Override
    public Row14<String, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long> valuesRow() {
        return (Row14) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return Playtime.PLAYTIME.UUID;
    }

    @Override
    public Field<Long> field2() {
        return Playtime.PLAYTIME.HUB;
    }

    @Override
    public Field<Long> field3() {
        return Playtime.PLAYTIME.SURVIVAL;
    }

    @Override
    public Field<Long> field4() {
        return Playtime.PLAYTIME.ATTACK;
    }

    @Override
    public Field<Long> field5() {
        return Playtime.PLAYTIME.SANDBOX;
    }

    @Override
    public Field<Long> field6() {
        return Playtime.PLAYTIME.PVP;
    }

    @Override
    public Field<Long> field7() {
        return Playtime.PLAYTIME.ANNEXATION;
    }

    @Override
    public Field<Long> field8() {
        return Playtime.PLAYTIME.ANARCHY;
    }

    @Override
    public Field<Long> field9() {
        return Playtime.PLAYTIME.CAMPAIGN_MAPS;
    }

    @Override
    public Field<Long> field10() {
        return Playtime.PLAYTIME.MS_GO;
    }

    @Override
    public Field<Long> field11() {
        return Playtime.PLAYTIME.HEX_PVP;
    }

    @Override
    public Field<Long> field12() {
        return Playtime.PLAYTIME.CASTLE_WARS;
    }

    @Override
    public Field<Long> field13() {
        return Playtime.PLAYTIME.CRAWLER_ARENA;
    }

    @Override
    public Field<Long> field14() {
        return Playtime.PLAYTIME.ZONE_CAPTURE;
    }

    @Override
    public String component1() {
        return getUuid();
    }

    @Override
    public Long component2() {
        return getHub();
    }

    @Override
    public Long component3() {
        return getSurvival();
    }

    @Override
    public Long component4() {
        return getAttack();
    }

    @Override
    public Long component5() {
        return getSandbox();
    }

    @Override
    public Long component6() {
        return getPvp();
    }

    @Override
    public Long component7() {
        return getAnnexation();
    }

    @Override
    public Long component8() {
        return getAnarchy();
    }

    @Override
    public Long component9() {
        return getCampaignMaps();
    }

    @Override
    public Long component10() {
        return getMsGo();
    }

    @Override
    public Long component11() {
        return getHexPvp();
    }

    @Override
    public Long component12() {
        return getCastleWars();
    }

    @Override
    public Long component13() {
        return getCrawlerArena();
    }

    @Override
    public Long component14() {
        return getZoneCapture();
    }

    @Override
    public String value1() {
        return getUuid();
    }

    @Override
    public Long value2() {
        return getHub();
    }

    @Override
    public Long value3() {
        return getSurvival();
    }

    @Override
    public Long value4() {
        return getAttack();
    }

    @Override
    public Long value5() {
        return getSandbox();
    }

    @Override
    public Long value6() {
        return getPvp();
    }

    @Override
    public Long value7() {
        return getAnnexation();
    }

    @Override
    public Long value8() {
        return getAnarchy();
    }

    @Override
    public Long value9() {
        return getCampaignMaps();
    }

    @Override
    public Long value10() {
        return getMsGo();
    }

    @Override
    public Long value11() {
        return getHexPvp();
    }

    @Override
    public Long value12() {
        return getCastleWars();
    }

    @Override
    public Long value13() {
        return getCrawlerArena();
    }

    @Override
    public Long value14() {
        return getZoneCapture();
    }

    @Override
    public PlaytimeRecord value1(String value) {
        setUuid(value);
        return this;
    }

    @Override
    public PlaytimeRecord value2(Long value) {
        setHub(value);
        return this;
    }

    @Override
    public PlaytimeRecord value3(Long value) {
        setSurvival(value);
        return this;
    }

    @Override
    public PlaytimeRecord value4(Long value) {
        setAttack(value);
        return this;
    }

    @Override
    public PlaytimeRecord value5(Long value) {
        setSandbox(value);
        return this;
    }

    @Override
    public PlaytimeRecord value6(Long value) {
        setPvp(value);
        return this;
    }

    @Override
    public PlaytimeRecord value7(Long value) {
        setAnnexation(value);
        return this;
    }

    @Override
    public PlaytimeRecord value8(Long value) {
        setAnarchy(value);
        return this;
    }

    @Override
    public PlaytimeRecord value9(Long value) {
        setCampaignMaps(value);
        return this;
    }

    @Override
    public PlaytimeRecord value10(Long value) {
        setMsGo(value);
        return this;
    }

    @Override
    public PlaytimeRecord value11(Long value) {
        setHexPvp(value);
        return this;
    }

    @Override
    public PlaytimeRecord value12(Long value) {
        setCastleWars(value);
        return this;
    }

    @Override
    public PlaytimeRecord value13(Long value) {
        setCrawlerArena(value);
        return this;
    }

    @Override
    public PlaytimeRecord value14(Long value) {
        setZoneCapture(value);
        return this;
    }

    @Override
    public PlaytimeRecord values(String value1, Long value2, Long value3, Long value4, Long value5, Long value6, Long value7, Long value8, Long value9, Long value10, Long value11, Long value12, Long value13, Long value14) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PlaytimeRecord
     */
    public PlaytimeRecord() {
        super(Playtime.PLAYTIME);
    }

    /**
     * Create a detached, initialised PlaytimeRecord
     */
    public PlaytimeRecord(String uuid, Long hub, Long survival, Long attack, Long sandbox, Long pvp, Long annexation, Long anarchy, Long campaignMaps, Long msGo, Long hexPvp, Long castleWars, Long crawlerArena, Long zoneCapture) {
        super(Playtime.PLAYTIME);

        setUuid(uuid);
        setHub(hub);
        setSurvival(survival);
        setAttack(attack);
        setSandbox(sandbox);
        setPvp(pvp);
        setAnnexation(annexation);
        setAnarchy(anarchy);
        setCampaignMaps(campaignMaps);
        setMsGo(msGo);
        setHexPvp(hexPvp);
        setCastleWars(castleWars);
        setCrawlerArena(crawlerArena);
        setZoneCapture(zoneCapture);
    }
}
