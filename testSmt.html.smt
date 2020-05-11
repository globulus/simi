<html>
    <body>
        <%! docWideVal = 5 %>
        Value is: <b><%= value %></b> (should be 3) <%# This is a comment %>
        <ul>
            %%for i in docWideVal.times() {
                <li>Loop value is <%= i %>%_
                %%if i % 2 {
                    %_ odd%_
                %%}
                %%else {
                    %_ even%_
                %%}
                %_ number</li>
            %%}
        </ul>
    <body>
</html>
