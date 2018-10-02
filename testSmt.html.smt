<html>
    <body>
        Value is: <b><%= value %></b> (should be 3)
        <ul>
            %%for i in 5.times():
                <li>Loop value is <%= i %>%_
                %%if i % 2:
                    %_ odd%_
                %%end
                %%else:
                    %_ even%_
                %%end
                %_ number</li>
            %%end
        </ul>
    <body>
</html>
