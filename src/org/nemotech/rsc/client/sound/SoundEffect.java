package org.nemotech.rsc.client.sound;
   
/**
 * This enumeration encapsulates all the sound effects of the game.
 * 
 * 1. Define all your sound effect names and the associated wave file.
 * 2. To play a specific sound, simply invoke SoundEffect.SOUND_NAME.play().
 * 3. You might optionally invoke the static method SoundEffect.init() to pre-load all the
 *    sound files, so that the play is not paused while loading the file for the first time.
 * 4. You can use the static variable SoundEffect.volume to mute the sound.
 */
public enum SoundEffect {
    
    ADVANCE      ("advance.wav"),    /* leveling up a skill */
    ANVIL        ("anvil.wav"),      /* smithing on an anvil */
    CHISEL       ("chisel.wav"),     /* cutting gems */
    CLICK        ("click.wav"),      /* equiping/unequipping items */
    CLOSE_DOOR   ("closedoor.wav"),  /* closing a door */
    COINS        ("coins.wav"),      /* buying items from a shop */
    COMBAT_1A    ("combat1a.wav"),   /* normal combat miss */
    COMBAT_1B    ("combat1b.wav"),   /* normal combat hit */
    COMBAT_2A    ("combat2a.wav"),   /* undead combat miss */
    COMBAT_2B    ("combat2b.wav"),   /* undead combat hit */
    COMBAT_3A    ("combat3a.wav"),   /* armor combat miss */
    COMBAT_3B    ("combat3b.wav"),   /* armor combat hit */
    COOKING      ("cooking.wav"),    /* cooking food on a range or fire */
    DEATH        ("death.wav"),      /* dying to an enemy */
    DROP_OBJECT  ("dropobject.wav"), /* dropping an item */
    EAT          ("eat.wav"),        /* eating food */
    FILL_JUG     ("filljug.wav"),    /* filling buckets/jugs/pots with water */
    FISH         ("fish.wav"),       /* fishing */
    FOUND_GEM    ("foundgem.wav"),   /* finding a gem while mining */
    MECHANICAL   ("mechanical.wav"), /* using a spinning wheel */
    MINE         ("mine.wav"),       /* mining ore */
    OPEN_DOOR    ("opendoor.wav"),   /* opening a door */
    OUT_OF_AMMO  ("outofammo.wav"),  /* running out of bolts or arrows while ranging an enemy */
    POTATO       ("potato.wav"),     /* picking food from plants or trees */
    PRAYER_OFF   ("prayeroff.wav"),  /* turning a prayer off */
    PRAYER_ON    ("prayeron.wav"),   /* turning a prayer on */
    PROSPECT     ("prospect.wav"),   /* prospecting a mineral rock */
    RECHARGE     ("recharge.wav"),   /* recharging prayer points at an altar */
    RETREAT      ("retreat.wav"),    /* enemy retreating from battle */
    SECRET_DOOR  ("secretdoor.wav"), /* entering a secret door */
    SHOOT        ("shoot.wav"),      /* ranging an enemy with a bow/crossbow */
    SPELL_FAIL   ("spellfail.wav"),  /* failing to cast a magic spell */
    SPELL_OK     ("spellok.wav"),    /* casting a magic spell successfully */
    TAKE_OBJECT  ("takeobject.wav"), /* picking up ground items */
    UNDER_ATTACK ("underattack.wav"),/* being attacked by an enemy */
    VICTORY      ("victory.wav");    /* defeating an enemy */
   
    /* nested class for specifying volume (kept for API compatibility) */
    public static enum Volume {
        MUTE, LOW, MEDIUM, HIGH
    }

    /* force muted by default; sound system is disabled */
    public static Volume volume = Volume.MUTE;

    /* constructor to construct each element of the enum with its own sound file */
    SoundEffect(String fileName) {
        /* Audio is disabled; ignore fileName and avoid initializing audio resources */
    }

    /* play or replay the sound effect from the beginning, by rewinding */
    public void play() {
        /* No-op: sound effects are disabled */
    }

    /* optional static method to pre-load all the sound files */
    public static void init() {
        /* No-op: keep for compatibility */
    }
    
}
