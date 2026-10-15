package com.anticipate.listr.scheduler.entities;

import jakarta.persistence.*;

/*  A person who can be rostered as station master
 *
 *  Deliberately separate from the login User entity, since
 *  rostered staff don't need an account on this server.
 */
@Table(name = "station_masters")
@Entity
public class StationMaster
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false)
    private String displayName;

    // Zoho Cliq user ID, used to @mention them in the channel
    @Column(nullable = false, unique = true)
    private String cliqUserId;

    public Integer getId()
    {
        return id;
    }

    public StationMaster setId(Integer id)
    {
        this.id = id;
        return this;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public StationMaster setDisplayName(String displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public String getCliqUserId()
    {
        return cliqUserId;
    }

    public StationMaster setCliqUserId(String cliqUserId)
    {
        this.cliqUserId = cliqUserId;
        return this;
    }
}
