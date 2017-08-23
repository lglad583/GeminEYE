package vision.gemineye.model.entity;

import org.w3c.dom.Attr;
import sun.util.calendar.BaseCalendar;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Calendar.DATE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

public class Identity {
    Date year = Calendar.getInstance().getTime();

    public Identity(Name name, Date dob, Attribute gender, Attribute race) {
        this.name = name;
        this.dob = dob;
        this.race = race;
        this.gender = gender;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }
    public int getAge(){
        Calendar current = GregorianCalendar.getInstance();
        Calendar birthYear = GregorianCalendar.getInstance();
        current.setTime(year);
        birthYear.setTime(dob);

        int diff = current.get(YEAR) - birthYear.get(YEAR);
        if (birthYear.get(MONTH) > current.get(MONTH) ||
                (birthYear.get(MONTH) == current.get(MONTH) && birthYear.get(DATE) > current.get(DATE))) {
            diff--;
        }
        return diff;

    }
    public Attribute getRace() {
        return race;
    }

    public void setRace(Attribute race) {
        this.race = race;
    }

    public Attribute getGender() {
        return gender;
    }

    public void setGender(Attribute gender) {
        this.gender = gender;
    }

    private Name name;
    private Date dob;
    private Attribute race;
    private Attribute gender;

    public String getDobAsText() {
        if(dob == null) {
            return "--";
        }
        return dob.toString();
    }
}
