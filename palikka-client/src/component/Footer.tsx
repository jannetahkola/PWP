import {Grid, IconButton} from "@mui/material";
import {GitHub} from "@mui/icons-material";

function Footer() {
    return (
      <Grid
          container
          alignItems={"center"}
          direction={"column"}>
          <div className={"font-subtext"}>
              &copy; { new Date().getFullYear() } Janne Tahkola
          </div>
          <div>
              <a
                  href={ "https://github.com/jannetahkola" }
                  target={ "_blank" }
                  rel={ "noreferrer" }>
                  <IconButton>
                      <GitHub/>
                  </IconButton>
              </a>
          </div>
      </Grid>
    );
}

export default Footer;