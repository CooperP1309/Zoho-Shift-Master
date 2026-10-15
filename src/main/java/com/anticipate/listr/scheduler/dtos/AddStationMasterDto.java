package com.anticipate.listr.scheduler.dtos;

public class AddStationMasterDto
{

    private String displayName;

    private String cliqUserId;

    public String getDisplayName()
    {
        return displayName;
    }

    public AddStationMasterDto setDisplayName(String displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public String getCliqUserId()
    {
        return cliqUserId;
    }

    public AddStationMasterDto setCliqUserId(String cliqUserId)
    {
        this.cliqUserId = cliqUserId;
        return this;
    }

    public AddStationMasterDto()
    {

    }
}
